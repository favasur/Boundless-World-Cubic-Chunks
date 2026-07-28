package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedCubeGenerator;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * Server-side cube provider. 1.21 port: replaces the bespoke {@code XYZMap}
 * (which we did not port over) with our {@link CubeMap}; switches the main
 * thread executor off the deprecated server execute hook to
 * {@code MinecraftServer.executeIfPossible}; uses {@code Util.backgroundExecutor}
 * instead of the legacy one-shot fork join pool.
 */
public class CubeProviderServer implements ICubeProviderServer, ICubeProviderInternal {
    public Cube getCube(CubePos pos) {
        return this.getCube(pos.getX(), pos.getY(), pos.getZ());
    }

    public Cube getCube(CubePos pos, Requirement req) {
        return this.getCube(pos.getX(), pos.getY(), pos.getZ(), req);
    }

    @Nullable
    public Cube getCubeNow(CubePos pos, Requirement req) {
        return this.getCubeNow(pos.getX(), pos.getY(), pos.getZ(), req);
    }

    @Nonnull
    private final ServerLevel level;
    @Nonnull
    private final ICubeGenerator cubeGen;
    @Nonnull
    private final CubeMap cubeMap = new CubeMap();
    @Nullable
    private final ICubeIO cubeIO;
    @Nonnull
    private final BlankCube emptyCube;
    @Nonnull
    private final EmptyColumn emptyColumn;
    @Nonnull
    private final Map<CubePos, CompletableFuture<Cube>> pendingTasks = new ConcurrentHashMap<>();
    @Nonnull
    private final Executor mainThreadExecutor;
    @Nonnull
    private final PlayerCubeMap playerCubeMap;

    public CubeProviderServer(ServerLevel level, ICubeGenerator cubeGen, @Nullable ICubeIO cubeIO) {
        this.level = level;
        this.cubeGen = cubeGen;
        this.cubeIO = cubeIO;
        this.emptyColumn = new EmptyColumn(level, 0, 0);
        this.emptyCube = new BlankCube(this.emptyColumn);
        this.mainThreadExecutor = r -> level.getServer().executeIfPossible(r);
        this.playerCubeMap = new PlayerCubeMap(level, this);
    }

    public PlayerCubeMap getPlayerCubeMap() {
        return this.playerCubeMap;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ICubeGenerator getCubeGenerator() {
        return this.cubeGen;
    }

    @Nullable
    public ICubeIO getCubeIO() {
        return this.cubeIO;
    }

    @Override
    @Nullable
    public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
        return this.cubeMap.get(cubeX, cubeY, cubeZ);
    }

    @Nullable
    public Cube getLoadedCube(CubePos pos) {
        return this.getLoadedCube(pos.getX(), pos.getY(), pos.getZ());
    }

    public Cube getCube(int cubeX, int cubeY, int cubeZ) {
        return this.getCube(cubeX, cubeY, cubeZ, Requirement.GENERATE);
    }

    @Override
    public Cube getCube(int cubeX, int cubeY, int cubeZ, Requirement req) {
        Cube loaded = this.getLoadedCube(cubeX, cubeY, cubeZ);
        if (loaded != null && meetsRequirement(loaded, req)) {
            return loaded;
        }
        // Avoid deadlock on the server thread: run synchronously if we are on the main thread.
        if (this.level.getServer().isSameThread()) {
            return this.loadAndFinishCubeSync(cubeX, cubeY, cubeZ, req);
        }
        return this.getCubeFuture(cubeX, cubeY, cubeZ, req).join();
    }

    public Cube getCubeNow(int cubeX, int cubeY, int cubeZ, Requirement req) {
        return this.getLoadedCube(cubeX, cubeY, cubeZ);
    }

    public boolean isCubePending(CubePos pos) {
        return this.pendingTasks.containsKey(pos);
    }

    public CompletableFuture<Cube> getCubeFuture(int cubeX, int cubeY, int cubeZ, Requirement req) {
        CubePos pos = CubePos.of(cubeX, cubeY, cubeZ);
        Cube loaded = this.getLoadedCube(pos);
        if (loaded != null && meetsRequirement(loaded, req)) {
            return CompletableFuture.completedFuture(loaded);
        }
        return this.pendingTasks.computeIfAbsent(pos, p ->
                CompletableFuture.supplyAsync(() -> this.loadOrGenerateCubeBackground(cubeX, cubeY, cubeZ), Util.backgroundExecutor())
                        .thenApplyAsync(cube -> this.finishCubeOnMainThread(cube, req), this.mainThreadExecutor)
                        .whenComplete((cube, ex) -> {
                            if (ex != null) {
                                this.pendingTasks.remove(pos);
                            }
                        })
        );
    }

    private static boolean meetsRequirement(Cube cube, Requirement req) {
        return switch (req) {
            case GET_CACHED, LOAD, GENERATE -> true;
            case POPULATE -> cube.isFullyPopulated();
            case LIGHT -> cube.isFullyPopulated() && cube.isInitialLightingDone();
        };
    }

    @Nullable
    private Cube loadAndFinishCubeSync(int cubeX, int cubeY, int cubeZ, Requirement req) {
        Cube cube = this.loadOrGenerateCubeBackground(cubeX, cubeY, cubeZ);
        return this.finishCubeOnMainThread(cube, req);
    }

    @Nullable
    private Cube loadOrGenerateCubeBackground(int cubeX, int cubeY, int cubeZ) {
        ChunkAccess column = this.getColumn(cubeX, cubeZ, Requirement.GENERATE);
        if (column == null || column instanceof EmptyColumn) {
            return this.emptyCube;
        }
        // EmptyColumn is a LevelChunk now, so the instanceof above is a stricter check.

        if (!(column instanceof LevelChunk levelChunk)) {
            return null;
        }

        // Try loading from disk first.
        if (this.cubeIO != null) {
            try {
                ICubeIO.PartialData<ICube> data = this.cubeIO.loadCubeNbt(column, cubeY);
                this.cubeIO.loadCubeAsyncPart(data, column, cubeY);
                this.cubeIO.loadCubeSyncPart(data);
                if (data.getObject() instanceof Cube loadedCube) {
                    return loadedCube;
                }
            } catch (Exception e) {
                CubicChunks.LOGGER.error("Failed to load cube at ({},{},{}), regenerating", cubeX, cubeY, cubeZ, e);
            }
        }

        // Per-invocation primer — never shared across threads.
        // The old shared field caused race conditions: one thread would reset
        // biomes3d to null while another was mid-generation, crashing with NPE.
        CubePrimer primer = new CubePrimer();
        Optional<CubePrimer> result = this.cubeGen.tryGenerateCube(cubeX, cubeY, cubeZ, primer, true);
        if (result.isEmpty() || result.get().isEmpty()) {
            // Empty space: don't allocate a Cube object. The cube will be created on demand
            // when the player places a non-air block here.
            return null;
        }
        Cube cube = new Cube(levelChunk, cubeY, result.get());
        return cube;
    }

    @Nullable
    private Cube finishCubeOnMainThread(@Nullable Cube cube, Requirement req) {
        if (cube == null || cube == this.emptyCube) {
            return cube;
        }
        CubePos pos = cube.getCoords();
        Cube existing = this.getLoadedCube(pos);
        if (existing != null) {
            return existing;
        }
        this.onCubeLoaded(cube);

        if (!cube.isFullyPopulated() && req.ordinal() >= Requirement.POPULATE.ordinal()) {
            this.populateCube(cube);
        }
        if ((!cube.isInitialLightingDone() || !cube.isSurfaceTracked()) && req.ordinal() >= Requirement.LIGHT.ordinal()) {
            this.calculateInitialLighting(cube);
        }
        this.pendingTasks.remove(pos);
        return cube;
    }

    @Override
    @Nullable
    public ChunkAccess getLoadedColumn(int columnX, int columnZ) {
        return this.level.getChunkSource().getChunk(columnX, columnZ, false);
    }

    @Override
    public ChunkAccess provideColumn(int columnX, int columnZ) {
        return this.getColumn(columnX, columnZ, Requirement.GENERATE);
    }

    @Override
    @Nullable
    public ChunkAccess getColumn(int columnX, int columnZ, Requirement req) {
        if (req == Requirement.GET_CACHED) {
            return this.getLoadedColumn(columnX, columnZ);
        }
        ChunkAccess chunk = this.level.getChunkSource().getChunk(columnX, columnZ, true);
        return chunk != null ? (ChunkAccess) chunk : (ChunkAccess) this.emptyColumn;
    }

    @Override
    public boolean isCubeGenerated(int cubeX, int cubeY, int cubeZ) {
        return this.getLoadedCube(cubeX, cubeY, cubeZ) != null
                || (this.cubeIO != null && this.cubeIO.cubeExists(cubeX, cubeY, cubeZ));
    }

    private void onCubeLoaded(Cube cube) {
        this.cubeMap.put(cube);
        IColumn column = cube.getColumn();
        if (column != null && !column.getLoadedCubes().contains(cube)) {
            column.addCube(cube);
            cube.onLoad();
        }
    }

    private void populateCube(Cube cube) {
        this.cubeGen.populate(cube);
        cube.setFullyPopulated(true);
    }

    private void calculateInitialLighting(Cube cube) {
        if (LightingManager.NO_SUNLIGHT_PROPAGATION) {
            cube.setInitialLightingDone(true);
            return;
        }
        if (!cube.isSurfaceTracked()) {
            cube.trackSurface();
        }
        FirstLightProcessor.forLevel(this.level).diffuseSkylight(cube);
        cube.setInitialLightingDone(true);
    }

    @Override
    public void tick(BooleanSupplier hasMoreTime) {
        int viewDistance = this.level.getServer().getPlayerList().getViewDistance();
        this.playerCubeMap.setViewDistance(viewDistance);

        this.playerCubeMap.tick();

        // Lazy band activation: check if any player is approaching a stacked
        // sub-dim's Y band. Once activated, the band generates cubes normally.
        if (CubicChunksConfig.stackingDimensionsEnabled
                && this.cubeGen instanceof StackedCubeGenerator stacked) {
            stacked.checkPlayerProximity();
        }

        Iterator<Cube> it = this.cubesIterator();
        while (it.hasNext()) {
            Cube cube = it.next();
            if (cube.needsSaving() && this.cubeIO != null) {
                this.cubeIO.saveCube(cube);
            }
        }
    }

    public boolean tryUnloadCube(Cube cube) {
        if (!cube.getTickets().canUnload()) {
            return false;
        }
        cube.onUnload();
        if (cube.needsSaving() && this.cubeIO != null) {
            this.cubeIO.saveCube(cube);
        }
        CubePos pos = cube.getCoords();
        this.cubeMap.remove(pos.getX(), pos.getY(), pos.getZ());
        IColumn column = cube.getColumn();
        column.removeCube(pos.getY());
        return true;
    }

    public int getLoadedCubeCount() {
        return this.cubeMap.size();
    }

    public Iterator<Cube> cubesIterator() {
        return this.cubeMap.all().iterator();
    }

    public java.util.Collection<Cube> getLoadedCubes() {
        return this.cubeMap.all();
    }

    public void addLoadedCube(ICube cube) {
        if (!(cube instanceof Cube serverCube)) {
            return;
        }
        if (this.getLoadedCube(serverCube.getCoords()) != null) {
            return;
        }
        this.onCubeLoaded(serverCube);
        this.playerCubeMap.onCubeCreated(serverCube);
    }

    @Override
    public void close() {
        Iterator<Cube> it = this.cubesIterator();
        while (it.hasNext()) {
            Cube cube = it.next();
            if (cube.needsSaving() && this.cubeIO != null) {
                try {
                    this.cubeIO.saveCube(cube);
                } catch (Exception e) {
                    CubicChunks.LOGGER.error("Failed to save cube {} during shutdown", cube.getCoords(), e);
                }
            }
        }
        if (this.cubeIO != null) {
            try {
                this.cubeIO.close();
            } catch (Exception e) {
                CubicChunks.LOGGER.error("Failed to close cube IO", e);
            }
        }
    }
}
