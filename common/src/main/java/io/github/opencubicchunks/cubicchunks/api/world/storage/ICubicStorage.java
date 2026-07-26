package io.github.opencubicchunks.cubicchunks.api.world.storage;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nonnull;
import java.io.Flushable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage
public interface ICubicStorage extends Flushable, AutoCloseable {

    boolean columnExists(ChunkPos pos) throws IOException;

    boolean cubeExists(CubePos pos) throws IOException;

    CompoundTag readColumn(ChunkPos pos) throws IOException;

    CompoundTag readCube(CubePos pos) throws IOException;

    void writeColumn(ChunkPos pos, CompoundTag tag) throws IOException;

    void writeCube(CubePos pos, CompoundTag tag) throws IOException;

    void forEachColumn(Consumer<ChunkPos> consumer) throws IOException;

    void forEachCube(Consumer<CubePos> consumer) throws IOException;

    @Nonnull PosBatch existsBatch(PosBatch positions) throws IOException;

    @Nonnull NBTBatch readBatch(PosBatch positions) throws IOException;

    void writeBatch(NBTBatch batch) throws IOException;

    @Override
    void flush() throws IOException;

    @Override
    void close() throws IOException;

    final class NBTBatch {
        public final Map<ChunkPos, CompoundTag> columns;
        public final Map<CubePos, CompoundTag> cubes;

        public NBTBatch(Map<ChunkPos, CompoundTag> columns, Map<CubePos, CompoundTag> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }

    final class PosBatch {
        public final Set<ChunkPos> columns;
        public final Set<CubePos> cubes;

        public PosBatch(Set<ChunkPos> columns, Set<CubePos> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }
}
