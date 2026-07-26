package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.chunkio.IONbtWriter / IONbtReader
public class CubeSerializer {
    public static final int DATA_VERSION = 1;

    private CubeSerializer() {
    }

    public static CompoundTag write(Cube cube, HolderLookup.Provider registries) {
        CompoundTag root = commonCubeHeader(cube);
        LevelChunkSection storage = cube.getStorage();
        if (storage != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            storage.write(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            root.putByteArray("section_bytes", bytes);
        }
        writeBlockEntities(cube, registries, root);
        writeEntities(cube, root);
        writeBiomes(cube, root);
        return root;
    }

    public static CompoundTag writeCubeFull(Cube cube) {
        return write(cube, cube.getWorld().registryAccess());
    }

    public static CompoundTag writeColumnFull(ChunkAccess column) {
        CompoundTag root = new CompoundTag();
        root.putInt("x", column.getPos().x);
        root.putInt("z", column.getPos().z);
        root.putInt("DataVersion", DATA_VERSION);
        // columns are saved through vanilla RegionFileStorage separately; this NBT
        // is the door cube/column-state-only record used by AsyncBatchingCubeIO.
        return root;
    }

    @Nullable
    public static Cube read(CompoundTag root, LevelChunk column) {
        int y = root.getInt("y");
        Cube cube = new Cube(column, y);

        if (root.contains("section_bytes", ListTag.TAG_BYTE_ARRAY)) {
            byte[] bytes = root.getByteArray("section_bytes");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            LevelChunkSection storage = new LevelChunkSection(column.getLevel().registryAccess().registryOrThrow(Registries.BIOME));
            storage.read(buf);
            cube.setStorage(storage);
        }

        cube.setPopulated(root.getBoolean("populated"));
        cube.setFullyPopulated(root.getBoolean("fullyPopulated"));
        cube.setSurfaceTracked(root.getBoolean("surfaceTracked"));
        cube.setInitialLightingDone(root.getBoolean("initialLightingDone"));

        readBlockEntities(root, cube, column.getLevel().registryAccess());
        readEntities(root, cube, column.getLevel());
        readBiomes(root, cube);

        return cube;
    }

    @Nullable
    public static ChunkAccess readColumnAsyncPart(CompoundTag nbt, int chunkX, int chunkZ) {
        if (nbt == null) return null;
        // Stub: column chunks are loaded through vanilla's own IOWorker in 1.21.
        // The compound tag shape for a column carries the same x/z and DataVersion
        // we wrote; production-grade reconstruction happens through MixinServerChunkCache.
        return null;
    }

    public static void readColumnSyncPart(ChunkAccess chunk, @Nullable CompoundTag nbt) {
        if (chunk instanceof LevelChunk levelChunk) {
            // Mark loaded through vanilla's gotten thread; nothing additional to read here.
            levelChunk.setUnsaved(false);
        }
    }

    public static void readCubeSyncPart(Cube cube, CompoundTag nbt, HolderLookup.Provider registries) {
        if (!cube.isCubeLoaded()) {
            CubeSerializer.read(nbt, (LevelChunk) cube.getColumn());
        }
    }

    private static CompoundTag commonCubeHeader(Cube cube) {
        CompoundTag root = new CompoundTag();
        root.putInt("x", cube.getX());
        root.putInt("y", cube.getY());
        root.putInt("z", cube.getZ());
        root.putInt("DataVersion", DATA_VERSION);
        root.putBoolean("populated", cube.isPopulated());
        root.putBoolean("fullyPopulated", cube.isFullyPopulated());
        root.putBoolean("surfaceTracked", cube.isSurfaceTracked());
        root.putBoolean("initialLightingDone", cube.isInitialLightingDone());
        return root;
    }

    private static void writeBlockEntities(Cube cube, HolderLookup.Provider registries, CompoundTag root) {
        if (cube.getBlockEntityMap().isEmpty()) return;
        ListTag list = new ListTag();
        for (BlockEntity be : cube.getBlockEntityMap().values()) {
            list.add(be.saveWithFullMetadata(registries));
        }
        root.put("block_entities", list);
    }

    private static void writeEntities(Cube cube, CompoundTag root) {
        if (cube.getEntitySet().isEmpty()) return;
        ListTag list = new ListTag();
        for (Entity entity : cube.getEntitySet()) {
            CompoundTag tag = new CompoundTag();
            if (entity.save(tag)) list.add(tag);
        }
        root.put("entities", list);
    }

    private static void writeBiomes(Cube cube, CompoundTag root) {
        int[] biomes = cube.getBiomeArray();
        if (biomes != null) root.putIntArray("biomes", biomes);
    }

    private static void readBlockEntities(CompoundTag root, Cube cube, HolderLookup.Provider registries) {
        if (!root.contains("block_entities", ListTag.TAG_LIST)) return;
        ListTag list = root.getList("block_entities", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockEntity be = BlockEntity.loadStatic(pos, cube.getBlockState(pos), tag, registries);
            if (be != null) cube.getBlockEntityMap().put(pos, be);
        }
    }

    private static void readEntities(CompoundTag root, Cube cube, Level level) {
        if (!root.contains("entities", ListTag.TAG_LIST)) return;
        ListTag list = root.getList("entities", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Entity entity = EntityType.create(tag, level).orElse(null);
            if (entity != null) cube.addEntity(entity);
        }
    }

    private static void readBiomes(CompoundTag root, Cube cube) {
        if (root.contains("biomes", CompoundTag.TAG_INT_ARRAY)) {
            cube.setBiomeArray(root.getIntArray("biomes"));
        }
    }
}
