package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO
// 1.21: write-batching queue → ICubicStorage writes; cleaned internal API.
public class AsyncBatchingCubeIO implements ICubeIO {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Level world;
    protected final ICubicStorage storage;
    protected final Map<ChunkPos, CompoundTag> pendingColumns = new ConcurrentHashMap<>();
    protected final Map<CubePos, CompoundTag> pendingCubes = new ConcurrentHashMap<>();
    protected volatile boolean open = true;

    public AsyncBatchingCubeIO(Level world, ICubicStorage storage) throws IOException {
        this.world = Objects.requireNonNull(world, "world");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    protected void ensureOpen() {
        Preconditions.checkState(this.open, "AsyncBatchingCubeIO closed");
    }

    public ICubicStorage getStorage() {
        return this.storage;
    }

    @Override
    public boolean columnExists(int columnX, int columnZ) {
        ChunkPos pos = new ChunkPos(columnX, columnZ);
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            return this.pendingColumns.containsKey(pos) || safeExists(() -> this.storage.columnExists(pos));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
        CubePos pos = CubePos.of(cubeX, cubeY, cubeZ);
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            return this.pendingCubes.containsKey(pos) || safeExists(() -> this.storage.cubeExists(pos));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public PartialData<ChunkAccess> loadColumnNbt(int chunkX, int chunkZ) throws IOException {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            CompoundTag nbt = this.pendingColumns.get(pos);
            if (nbt == null) nbt = this.storage.readColumn(pos);
            return new PartialData<>(null, nbt);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public PartialData<ICube> loadCubeNbt(ChunkAccess column, int cubeY) throws IOException {
        CubePos pos = CubePos.of(column.getPos().x, cubeY, column.getPos().z);
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            CompoundTag nbt = this.pendingCubes.get(pos);
            if (nbt == null) nbt = this.storage.readCube(pos);
            return new PartialData<>(null, nbt);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void saveColumn(ChunkAccess column) {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            CompoundTag tag = CubeSerializer.writeColumnFull(column);
            if (tag == null) return;
            this.pendingColumns.put(column.getPos(), tag);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void saveCube(Cube cube) {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            CompoundTag tag = CubeSerializer.writeCubeFull(cube);
            if (tag == null) return;
            this.pendingCubes.put(cube.getCoords(), tag);
            cube.markSaved();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int getPendingColumnCount() {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            return this.pendingColumns.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int getPendingCubeCount() {
        this.lock.readLock().lock();
        try {
            this.ensureOpen();
            return this.pendingCubes.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void flush() throws IOException {
        this.lock.writeLock().lock();
        try {
            this.ensureOpen();
            Map<ChunkPos, CompoundTag> cols = new HashMap<>(this.pendingColumns);
            Map<CubePos, CompoundTag> cubes = new HashMap<>(this.pendingCubes);
            this.storage.writeBatch(new ICubicStorage.NBTBatch(cols, cubes));
            this.pendingColumns.clear();
            this.pendingCubes.clear();
            this.storage.flush();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        this.lock.writeLock().lock();
        try {
            this.ensureOpen();
            flush();
            this.storage.close();
            this.open = false;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void loadColumnAsyncPart(PartialData<ChunkAccess> info, int chunkX, int chunkZ) {
        if (info.getNbt() != null) {
            ChunkAccess chunk = CubeSerializer.readColumnAsyncPart(info.getNbt(), chunkX, chunkZ);
            info.setObject(chunk);
        }
    }

    @Override
    public void loadColumnSyncPart(PartialData<ChunkAccess> info) {
        if (info.getObject() != null && info.getNbt() != null) {
            CubeSerializer.readColumnSyncPart(info.getObject(), info.getNbt());
        }
    }

    @Override
    public void loadCubeAsyncPart(PartialData<ICube> info, ChunkAccess column, int cubeY) {
        if (info.getNbt() != null && column instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
            Cube cube = CubeSerializer.read(info.getNbt(), levelChunk);
            info.setObject(cube);
        }
    }

    @Override
    public void loadCubeSyncPart(PartialData<ICube> info) {
        if (info.getObject() instanceof Cube cube && info.getNbt() != null && cube.getColumn() instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
            CubeSerializer.read(info.getNbt(), levelChunk);
        }
    }

    private static boolean safeExists(Callable<Boolean> worker) {
        try {
            return worker.call();
        } catch (Exception e) {
            CubicChunks.LOGGER.error("IO existence check failed", e);
            return false;
        }
    }
}
