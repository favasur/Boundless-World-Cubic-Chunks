package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.Flushable;
import java.io.IOException;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO
public interface ICubeIO extends Flushable, AutoCloseable {
    @Override
    void flush() throws IOException;

    @Override
    void close() throws IOException;

    PartialData<ChunkAccess> loadColumnNbt(int chunkX, int chunkZ) throws IOException;

    void loadColumnAsyncPart(PartialData<ChunkAccess> data, int chunkX, int chunkZ);

    void loadColumnSyncPart(PartialData<ChunkAccess> data);

    PartialData<ICube> loadCubeNbt(ChunkAccess column, int cubeY) throws IOException;

    void loadCubeAsyncPart(PartialData<ICube> data, ChunkAccess column, int cubeY);

    void loadCubeSyncPart(PartialData<ICube> data);

    void saveColumn(ChunkAccess column);

    void saveCube(Cube cube);

    boolean cubeExists(int cubeX, int cubeY, int cubeZ);

    boolean columnExists(int columnX, int columnZ);

    int getPendingColumnCount();

    int getPendingCubeCount();

    class PartialData<T> {
        private CompoundTag nbt;
        private T object;

        public PartialData(T object, CompoundTag nbt) {
            this.object = object;
            this.nbt = nbt;
        }

        public T getObject() {
            return this.object;
        }

        public void setObject(T obj) {
            this.object = obj;
        }

        public CompoundTag getNbt() {
            return this.nbt;
        }

        public void setNbt(CompoundTag nbt) {
            this.nbt = nbt;
        }
    }
}
