package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Collection;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.WorldEncoder
// 1.21: I/O against vanilla LevelChunkSection / paletted container instead of
// ExtendedBlockStorage. The flag byte per cube is preserved.
public final class WorldEncoder {

    public static final byte FLAG_EMPTY = 1;
    public static final byte FLAG_HAS_STORAGE = 2;
    public static final byte FLAG_HAS_BIOMES = 4;

    private WorldEncoder() {
    }

    public static byte[] encodeCubes(Collection<Cube> cubes) {
        int size = computeEncodedSize(cubes);
        byte[] out = new byte[size];
        FriendlyByteBuf writeBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(out).writerIndex(0));

        for (Cube cube : cubes) {
            byte flags = 0;
            if (cube.isEmpty()) flags |= FLAG_EMPTY;
            if (cube.getStorage() != null && !cube.getStorage().hasOnlyAir()) flags |= FLAG_HAS_STORAGE;
            if (cube.getBiomeArray() != null) flags |= FLAG_HAS_BIOMES;
            writeBuf.writeByte(flags);
        }
        for (Cube cube : cubes) {
            if (!cube.isEmpty() && cube.getStorage() != null) {
                cube.getStorage().getStates().write(writeBuf);
            }
        }
        for (Cube cube : cubes) {
            byte[] blockLight = cube.getBlockLightData();
            if (blockLight != null) writeBuf.writeBytes(blockLight);
            else writeBuf.writeBytes(new byte[2048]);
        }
        for (Cube cube : cubes) {
            byte[] skyLight = cube.getSkyLightData();
            if (skyLight != null) writeBuf.writeBytes(skyLight);
            else writeBuf.writeBytes(new byte[2048]);
        }
        for (Cube cube : cubes) {
            if (!cube.isEmpty()) {
                byte[] heightmap = ((ServerHeightMap) ((IColumn) cube.getColumn()).getOpacityIndex()).getDataForClient();
                writeBuf.writeBytes(heightmap);
            }
        }
        for (Cube cube : cubes) {
            int[] biomes = cube.getBiomeArray();
            if (biomes != null) {
                for (int i : biomes) writeBuf.writeInt(i);
            }
        }
        return out;
    }

    public static int computeEncodedSize(Collection<Cube> cubes) {
        int size = cubes.size(); // flag bytes
        for (Cube cube : cubes) {
            if (!cube.isEmpty() && cube.getStorage() != null) {
                // Cannot pre-compute section byte-size without writing; allocate generously.
                size += 8192;
            }
            if (cube.getBlockLightData() != null) size += cube.getBlockLightData().length;
            if (cube.getSkyLightData() != null) size += cube.getSkyLightData().length;
            size += 1024; // heightmap
            if (cube.getBiomeArray() != null) {
                size += cube.getBiomeArray().length * 4;
            }
        }
        return size;
    }

    public static void decodeCubes(FriendlyByteBuf in, Collection<Cube> cubes) {
        boolean[] hasStorage = new boolean[cubes.size()];
        boolean[] hasBiomes = new boolean[cubes.size()];
        boolean[] isEmpty = new boolean[cubes.size()];
        Cube[] cubeArr = cubes.toArray(new Cube[0]);
        for (int i = 0; i < cubeArr.length; i++) {
            int flags = in.readByte() & 0xFF;
            isEmpty[i] = (flags & FLAG_EMPTY) != 0;
            hasStorage[i] = (flags & FLAG_HAS_STORAGE) != 0;
            hasBiomes[i] = (flags & FLAG_HAS_BIOMES) != 0;
        }
        for (int i = 0; i < cubeArr.length; i++) {
            if (isEmpty[i] || cubeArr[i] == null) continue;
            LevelChunk sectionLevel = (LevelChunk) cubeArr[i].getColumn();
            LevelChunkSection storage = new LevelChunkSection(
                    sectionLevel.getLevel().registryAccess().registryOrThrow(Registries.BIOME));
            storage.getStates().read(in);
            cubeArr[i].setStorage(storage);
            cubeArr[i].setBlockLightData(new byte[2048]);
            cubeArr[i].setSkyLightData(new byte[2048]);
            hasStorage[i] = true;
        }
        for (int i = 0; i < cubeArr.length; i++) {
            byte[] blockLight = new byte[2048];
            in.readBytes(blockLight);
            if (hasStorage[i] && cubeArr[i] != null) {
                cubeArr[i].setBlockLightData(blockLight);
            }
        }
        for (int i = 0; i < cubeArr.length; i++) {
            byte[] skyLight = new byte[2048];
            in.readBytes(skyLight);
            if (hasStorage[i] && cubeArr[i] != null) {
                cubeArr[i].setSkyLightData(skyLight);
            }
        }
        for (int i = 0; i < cubeArr.length; i++) {
            if (!isEmpty[i] && cubeArr[i] != null) {
                byte[] hmapData = new byte[1024];
                in.readBytes(hmapData);
                ClientHeightMap coi = (ClientHeightMap) ((IColumn) cubeArr[i].getColumn()).getOpacityIndex();
                coi.setData(hmapData);
            }
        }
        for (int i = 0; i < cubeArr.length; i++) {
            if (hasBiomes[i] && cubeArr[i] != null) {
                int[] biomes = new int[64];
                for (int j = 0; j < 64; j++) biomes[j] = in.readInt();
                cubeArr[i].setBiomeArray(biomes);
            }
        }
    }

    public static byte[] encodeColumn(LevelChunk column) {
        // 1.21: column data is delivered via vanilla SPacketChunkData. Return empty marker.
        return new byte[0];
    }

    public static void decodeColumn(FriendlyByteBuf in, LevelChunk column) {
        // no-op: column bytes piggy-back on vanilla packet in 1.21.
    }
}
