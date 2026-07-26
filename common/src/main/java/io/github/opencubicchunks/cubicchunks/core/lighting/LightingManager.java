package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager
public class LightingManager implements ILightingManager {
    public static final boolean NO_SUNLIGHT_PROPAGATION = "true".equalsIgnoreCase(System.getProperty("cubicchunks.nosunlight"));
    public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;

    @Nonnull
    private final Level level;
    private final LightPropagator lightPropagator = new LightPropagator();
    @Nonnull
    private final List<IHeightChangeListener> heightUpdateListeners = new ArrayList<>();
    @Nullable
    private LightUpdateTracker tracker;
    @Nonnull
    private final Set<CubeLightUpdateInfo> toUpdate = new HashSet<>();

    public LightingManager(Level level) {
        this.level = level;
    }

    @Nullable
    LightUpdateTracker getTracker() {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        if (this.tracker == null && !this.level.isClientSide && this.level.dimensionType().hasSkyLight()) {
            // PlayerCubeMap dependency is stubbed for common module.
            this.tracker = new LightUpdateTracker(null);
        }
        return this.tracker;
    }

    public void registerHeightChangeListener(IHeightChangeListener listener) {
        this.heightUpdateListeners.add(listener);
    }

    @Nullable
    public CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return null;
        }
        // Stacked band cubes whose entire Y range lives above the overworld's
        // getMaxBuildHeight() (=320) or below getMinBuildHeight() (=-64) are out of
        // reach of the vanilla LevelLightEngine. Skip allocating a tracker for them so
        // we never queue relightMultiBlock work that the engine will silently drop,
        // and so a future regression cannot open a path that pushes End-band skylight
        // down onto the overworld surface.
        int cubeMinY = cube.getCoords().getMinBlockY();
        int cubeMaxY = cube.getCoords().getMaxBlockY();
        if (cubeMinY >= this.level.getMaxBuildHeight() || cubeMaxY < this.level.getMinBuildHeight()) {
            return null;
        }
        return !cube.getWorld().dimensionType().hasSkyLight() ? null : new CubeLightUpdateInfo(cube, this);
    }

    private void columnSkylightUpdate(UpdateType type, LevelChunk column, int localX, int minY, int maxY, int localZ) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return;
        }
        // Block-change rows whose Y range is entirely above maxBuildHeight (=320) or
        // entirely below minBuildHeight (=-64) are out of the engine's range; drop the
        // call. This is the Last Line of Defence for "End islands must not cast shadows
        // onto the overworld surface": even if the engine were widened in future, we
        // explicitly opt stacked bands out of the overworld skylight pipeline.
        if (minY >= this.level.getMaxBuildHeight() || maxY < this.level.getMinBuildHeight()) {
            return;
        }
        if (this.level.dimensionType().hasSkyLight()) {
            int blockX = Coords.localToBlock(column.getPos().x, localX);
            int blockZ = Coords.localToBlock(column.getPos().z, localZ);
            // Clamp minY/maxY into the engine's range so we never feed a check past
            // the array bounds — this also guarantees we never propagate light
            // downward from a stacked band's block mutation into the overworld surface.
            int engineMinY = Math.min(this.level.getMaxBuildHeight() - 1, Math.max(this.level.getMinBuildHeight(), Math.min(minY, maxY)));
            int engineMaxY = Math.min(this.level.getMaxBuildHeight() - 1, Math.max(this.level.getMinBuildHeight(), Math.max(minY, maxY)));
            if (type == UpdateType.IMMEDIATE) {
                // 1.21 light propagation is delegated to the engine; schedule for recheck.
                this.level.getLightEngine().checkBlock(new BlockPos(blockX, engineMinY, blockZ));
                if (engineMaxY > engineMinY) {
                    this.level.getLightEngine().checkBlock(new BlockPos(blockX, engineMaxY, blockZ));
                }
            } else {
                ICube cube = ((IColumn) column).getCube(Coords.blockToCube(engineMinY));
                this.markCubeBlockColumnForUpdate(cube, blockX, blockZ);
            }
        }
    }

    @Override
    public void doOnBlockSetLightUpdates(LevelChunk column, int localX, int y1, int y2, int localZ) {
        this.columnSkylightUpdate(UpdateType.IMMEDIATE, column, localX, Math.min(y1, y2), Math.max(y1, y2), localZ);
    }

    @Override
    public void onTick() {
        Set<CubeLightUpdateInfo> updateSet = new HashSet<>(this.toUpdate);
        this.toUpdate.clear();
        int total = updateSet.size();
        long ms = -System.currentTimeMillis();
        Iterator<CubeLightUpdateInfo> iterator = updateSet.iterator();

        while (iterator.hasNext()) {
            CubeLightUpdateInfo cubeLightUpdateInfo = iterator.next();
            cubeLightUpdateInfo.tick();
            if (!cubeLightUpdateInfo.hasUpdates()) {
                iterator.remove();
            }
        }

        ms += System.currentTimeMillis();
        int updated = total - updateSet.size();
        if (ms > 50L && updated > 0) {
            CubicChunks.LOGGER.debug("Light tick: {} cubes, {} updated in {}ms, {}ms/cube", total, updated, ms, (double) ms / (double) updated);
        }

        this.toUpdate.addAll(updateSet);
        LightUpdateTracker tracker = this.getTracker();
        if (tracker != null) {
            tracker.sendAll();
        }
    }

    @Override
    public void markCubeBlockColumnForUpdate(ICube cube, int blockX, int blockZ) {
        CubeLightUpdateInfo data = ((Cube) cube).getCubeLightUpdateInfo();
        if (data != null) {
            data.markBlockColumnForUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
        }
    }

    @Override
    public boolean checkLightFor(LightLayer lightType, BlockPos pos) {
        if (!this.level.isLoaded(pos)) {
            return false;
        }
        LevelLightEngine lightEngine = this.level.getLightEngine();
        if (lightEngine != null) {
            lightEngine.checkBlock(pos);
        }
        return true;
    }

    void markToUpdate(CubeLightUpdateInfo cubeLightUpdateInfo) {
        this.toUpdate.add(cubeLightUpdateInfo);
    }

    boolean relightMultiBlock(BlockPos startPos, BlockPos endPos, LightLayer type, Consumer<BlockPos> notify) {
        if (NO_SUNLIGHT_PROPAGATION) {
            return true;
        }
        LevelLightEngine lightEngine = this.level.getLightEngine();
        if (lightEngine != null) {
            for (BlockPos pos : BlockPos.betweenClosed(startPos, endPos)) {
                lightEngine.checkBlock(pos);
                if (notify != null) {
                    notify.accept(pos);
                }
            }
        }
        return true;
    }

    public void sendHeightMapUpdate(BlockPos pos) {
        int size = this.heightUpdateListeners.size();
        for (int i = 0; i < size; i++) {
            this.heightUpdateListeners.get(i).heightUpdated(pos.getX(), pos.getZ());
        }
    }

    public static class CubeLightUpdateInfo {
        private final Cube cube;
        private final boolean[] toUpdateColumns = new boolean[256];
        private final LightingManager lightingManager;
        private boolean hasUpdates;
        public EnumSet<Direction> edgeNeedSkyLightUpdate = EnumSet.noneOf(Direction.class);

        public CubeLightUpdateInfo(Cube cube, LightingManager lm) {
            this.cube = cube;
            this.lightingManager = lm;
        }

        void markBlockColumnForUpdate(int localX, int localZ) {
            this.toUpdateColumns[this.index(localX, localZ)] = true;
            this.hasUpdates = true;
            this.lightingManager.markToUpdate(this);
        }

        public void markEdgeNeedSkyLightUpdate(Direction side) {
            this.edgeNeedSkyLightUpdate.add(side);
            this.lightingManager.markToUpdate(this);
        }

        public void tick() {
            if (NO_SUNLIGHT_PROPAGATION) {
                return;
            }
            ICubicWorldInternal cubicWorld = (ICubicWorldInternal) this.cube.getWorld();
            LightingManager manager = cubicWorld.getLightingManager();
            LightUpdateTracker tracker = manager.getTracker();
            ICubeProviderInternal cache = (ICubeProviderInternal) cubicWorld.getCubeCache();

            if (!this.edgeNeedSkyLightUpdate.isEmpty() && this.cube.getWorld().isLoaded(this.cube.getCoords().getCenterBlockPos())) {
                EnumSet<Direction> removed = EnumSet.noneOf(Direction.class);

                for (Direction dir : Direction.values()) {
                    if (this.edgeNeedSkyLightUpdate.contains(dir)) {
                        CubePos cpos = this.cube.getCoords();
                        Cube loadedCube = cache.getLoadedCube(cpos.getX() + dir.getStepX(), cpos.getY() + dir.getStepY(), cpos.getZ() + dir.getStepZ());
                        if (loadedCube != null && loadedCube.isInitialLightingDone()) {
                            int minX = cpos.getMinBlockX();
                            int minY = cpos.getMinBlockY();
                            int minZ = cpos.getMinBlockZ();
                            int maxX = cpos.getMaxBlockX();
                            int maxY = cpos.getMaxBlockY();
                            int maxZ = cpos.getMaxBlockZ();
                            switch (dir) {
                                case DOWN:
                                    maxY = --minY + 1;
                                    break;
                                case UP:
                                    maxY++;
                                    minY = maxY - 1;
                                    break;
                                case NORTH:
                                    maxZ = --minZ + 1;
                                    break;
                                case SOUTH:
                                    maxZ++;
                                    minZ = maxZ - 1;
                                    break;
                                case WEST:
                                    maxX = --minX + 1;
                                    break;
                                case EAST:
                                    maxX++;
                                    minX = maxX - 1;
                            }

                            BlockPos start = new BlockPos(minX, minY, minZ);
                            BlockPos end = new BlockPos(maxX, maxY, maxZ);
                            manager.relightMultiBlock(start, end, LightLayer.SKY, pos -> {
                                this.cube.getWorld().getLightEngine().checkBlock(pos);
                                if (tracker != null) {
                                    tracker.onUpdate(pos);
                                }
                            });
                            removed.add(dir);
                        }
                    }
                }

                for (Direction dirx : removed) {
                    this.edgeNeedSkyLightUpdate.remove(dirx);
                    CubePos cpos = this.cube.getCoords();
                    Cube loadedCube = cache.getLoadedCube(cpos.getX() + dirx.getStepX(), cpos.getY() + dirx.getStepY(), cpos.getZ() + dirx.getStepZ());
                    if (loadedCube != null) {
                        CubeLightUpdateInfo cubeLightUpdateInfo = loadedCube.getCubeLightUpdateInfo();
                        if (cubeLightUpdateInfo != null) {
                            cubeLightUpdateInfo.edgeNeedSkyLightUpdate.remove(dirx.getOpposite());
                        }
                    }
                }
            }

            if (this.hasUpdates) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        if (this.toUpdateColumns[this.index(localX, localZ)]) {
                            BlockPos start = new BlockPos(
                                    Coords.localToBlock(this.cube.getX(), localX),
                                    Coords.cubeToMinBlock(this.cube.getY()),
                                    Coords.localToBlock(this.cube.getZ(), localZ)
                            );
                            BlockPos end = new BlockPos(
                                    Coords.localToBlock(this.cube.getX(), localX),
                                    Coords.cubeToMaxBlock(this.cube.getY()),
                                    Coords.localToBlock(this.cube.getZ(), localZ)
                            );
                            manager.relightMultiBlock(start, end, LightLayer.SKY, pos -> {
                                this.cube.getWorld().getLightEngine().checkBlock(pos);
                                if (tracker != null) {
                                    tracker.onUpdate(pos);
                                }
                            });
                            this.toUpdateColumns[this.index(localX, localZ)] = false;
                        }
                    }
                }

                this.hasUpdates = false;
            }
        }

        private int index(int x, int z) {
            return x << 4 | z;
        }

        public boolean hasUpdates() {
            return this.hasUpdates || !this.edgeNeedSkyLightUpdate.isEmpty();
        }

        public void clear() {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    this.toUpdateColumns[this.index(localX, localZ)] = false;
                }
            }
            this.hasUpdates = false;
        }

        public void onUnload() {
            this.lightingManager.toUpdate.remove(this);
        }
    }

    public interface IHeightChangeListener {
        void heightUpdated(int x, int z);
    }

    private enum UpdateType {
        IMMEDIATE,
        QUEUED
    }
}
