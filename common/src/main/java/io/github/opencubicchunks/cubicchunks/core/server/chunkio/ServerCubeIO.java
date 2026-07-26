package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO
// 1.21: uses vanilla ChunkStorage per horizontal Y-layer for async region file IO.
public class ServerCubeIO implements ICubeIO {
    private final ServerLevel level;
    private final Path storageFolder;
    private final ConcurrentHashMap<Integer, ChunkStorage> workers = new ConcurrentHashMap<>();
    private volatile boolean open = true;

    public ServerCubeIO(ServerLevel level) {
        this.level = level;
        this.storageFolder = resolveStorageFolder(level);
    }

    public static Path resolveStorageFolder(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        String dimName = level.dimension().location().toString().replace(':', '_');
        return worldRoot.resolve("cubicchunks").resolve(dimName).resolve("cubes");
    }

    private ChunkStorage getWorker(int cubeY) {
        return this.workers.computeIfAbsent(cubeY, y -> {
            RegionStorageInfo info = new RegionStorageInfo(
                    this.level.dimension().location().toString(),
                    this.level.dimension(),
                    "cubicchunks"
            );
            return new ChunkStorage(info, this.storageFolder.resolve("layer_" + y), this.level.getServer().getFixerUpper(), true);
        });
    }

    @Override
    public PartialData<ChunkAccess> loadColumnNbt(int chunkX, int chunkZ) throws IOException {
        throw new UnsupportedOperationException("ServerCubeIO does not load columns; use vanilla chunk storage");
    }

    @Override
    public void loadColumnAsyncPart(PartialData<ChunkAccess> data, int chunkX, int chunkZ) {
        // No-op: vanilla handles columns.
    }

    @Override
    public void loadColumnSyncPart(PartialData<ChunkAccess> data) {
        // No-op.
    }

    @Override
    public PartialData<ICube> loadCubeNbt(ChunkAccess column, int cubeY) throws IOException {
        this.ensureOpen();
        CubePos pos = CubePos.of(column.getPos().x, cubeY, column.getPos().z);
        CompletableFuture<CompoundTag> future = this.getWorker(cubeY).read(new ChunkPos(pos.getX(), pos.getZ())).thenApply(opt -> opt.orElse(null));
        return new PartialData<>(null, future.join());
    }

    @Override
    public void loadCubeAsyncPart(PartialData<ICube> data, ChunkAccess column, int cubeY) {
        CompoundTag nbt = data.getNbt();
        if (nbt != null && column instanceof LevelChunk levelChunk) {
            Cube cube = CubeSerializer.read(nbt, levelChunk);
            data.setObject(cube);
        }
    }

    @Override
    public void loadCubeSyncPart(PartialData<ICube> data) {
        // Block entity and entity re-registration happens when cube.onLoad() is called by the provider.
    }

    @Override
    public void saveColumn(ChunkAccess column) {
        // Vanilla handles columns.
    }

    @Override
    public void saveCube(Cube cube) {
        this.ensureOpen();
        CompoundTag nbt = CubeSerializer.write(cube, this.level.registryAccess());
        CubePos pos = cube.getCoords();
        this.getWorker(pos.getY()).write(new ChunkPos(pos.getX(), pos.getZ()), nbt);
        cube.markSaved();
    }

    @Override
    public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
        this.ensureOpen();
        // Vanilla IOWorker has no synchronous existence check; attempt a load.
        CompletableFuture<CompoundTag> future = this.getWorker(cubeY).read(new ChunkPos(cubeX, cubeZ)).thenApply(opt -> opt.orElse(null));
        return future.join() != null;
    }

    @Override
    public boolean columnExists(int columnX, int columnZ) {
        return false;
    }

    @Override
    public int getPendingColumnCount() {
        return 0;
    }

    @Override
    public int getPendingCubeCount() {
        return 0;
    }

    @Override
    public void flush() throws IOException {
        this.ensureOpen();
        for (ChunkStorage worker : this.workers.values()) {
            worker.flushWorker();
        }
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        for (ChunkStorage worker : this.workers.values()) {
            worker.close();
        }
    }

    private void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("ServerCubeIO is already closed");
        }
    }
}
