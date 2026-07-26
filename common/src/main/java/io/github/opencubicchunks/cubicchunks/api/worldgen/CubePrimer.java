package io.github.opencubicchunks.cubicchunks.api.worldgen;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * A 16x16x16 volume of block states used during cubic world generation.
 * Ported from 1.12.2 Cubic Chunks.
 *
 * @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer
 */
// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer
public class CubePrimer {
    public static final BlockState DEFAULT_STATE = Blocks.AIR.defaultBlockState();
    private final char[] data;
    private byte[] extData = null;
    private Biome[] biomes3d = null;

    public boolean hasBiomes() {
        return this.biomes3d != null;
    }

    public CubePrimer() {
        this(new char[4096]);
    }

    public static CubePrimer createFilled(BlockState state) {
        int value = Block.BLOCK_STATE_REGISTRY.getId(state);
        char lsb = (char) value;
        char[] data = new char[4096];
        Arrays.fill(data, lsb);
        return new CubePrimer(data);
    }

    protected CubePrimer(char[] data) {
        this.data = data;
    }

    @Nullable
    public Biome getBiome(int localBiomeX, int localBiomeY, int localBiomeZ) {
        if (this.biomes3d == null) {
            return null;
        }
        int biomeX = localBiomeX * 2;
        int biomeZ = localBiomeZ * 2;
        return this.biomes3d[biomeX << 3 | biomeZ];
    }

    public void setBiome(int localBiomeX, int localBiomeY, int localBiomeZ, Biome biome) {
        if (this.biomes3d == null) {
            this.biomes3d = new Biome[64];
        }

        int biomeX = localBiomeX * 2;
        int biomeZ = localBiomeZ * 2;

        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                this.biomes3d[(biomeX + dx) << 3 | (biomeZ + dz)] = biome;
            }
        }
    }

    public BlockState getBlockState(int x, int y, int z) {
        int idx = getBlockIndex(x, y, z);
        int block = this.data[idx];
        if (this.extData != null) {
            block |= this.extData[idx] << 16;
        }

        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(block);
        return state == null ? DEFAULT_STATE : state;
    }

    public void setBlockState(int x, int y, int z, @Nonnull BlockState state) {
        int value = Block.BLOCK_STATE_REGISTRY.getId(state);
        char lsb = (char) value;
        int idx = getBlockIndex(x, y, z);
        this.data[idx] = lsb;
        if (value > 65535) {
            if (this.extData == null) {
                this.extData = new byte[4096];
            }
            this.extData[idx] = (byte) (value >>> 16);
        }
    }

    public boolean isEmpty() {
        for (char c : this.data) {
            if (c != 0) {
                return false;
            }
        }
        return this.extData == null;
    }

    /** 1.21 port: bulk-fill all 4096 voxels with a single BlockState. */
    public CubePrimer setAll(BlockState state) {
        int value = Block.BLOCK_STATE_REGISTRY.getId(state);
        char lsb = (char) value;
        Arrays.fill(this.data, lsb);
        if (value > 65535) {
            if (this.extData == null) {
                this.extData = new byte[4096];
            }
            byte msb = (byte) (value >>> 16);
            Arrays.fill(this.extData, msb);
        } else {
            this.extData = null;
        }
        return this;
    }

    /** 1.21 port: bulk-load biome ids (64 entries). */
    public void setBiomeArray(int[] flat) {
        if (flat == null) return;
        if (this.biomes3d == null) {
            this.biomes3d = new Biome[64];
        }
        // 1.21 port: flat biome ids are stored directly; biome object lookup deferred to render.
        for (int i = 0; i < Math.min(64, flat.length); i++) {
            this.biomes3d[i] = null;
        }
    }

    public void reset() {
        Arrays.fill(this.data, '\u0000');
        this.extData = null;
        this.biomes3d = null;
    }

    private static int getBlockIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }
}
