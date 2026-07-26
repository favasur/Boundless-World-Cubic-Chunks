package io.github.opencubicchunks.cubicchunks.api.util;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Immutable 3D cube position. A cube is a 16x16x16 volume.
 */
public final class CubePos implements XYZAddressable, Comparable<CubePos> {
    private final int x;
    private final int y;
    private final int z;

    private CubePos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static CubePos of(int x, int y, int z) {
        return CubePos.of(x, y, z);
    }

    public static CubePos fromBlockCoords(int blockX, int blockY, int blockZ) {
        return CubePos.of(blockX >> 4, blockY >> 4, blockZ >> 4);
    }

    public static CubePos fromChunk(int chunkX, int chunkY, int chunkZ) {
        return CubePos.of(chunkX, chunkY, chunkZ);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    public int getMinBlockX() {
        return Coords.cubeToMinBlock(x);
    }

    public int getMinBlockY() {
        return Coords.cubeToMinBlock(y);
    }

    public int getMinBlockZ() {
        return Coords.cubeToMinBlock(z);
    }

    public int getMaxBlockX() {
        return Coords.cubeToMaxBlock(x);
    }

    public int getMaxBlockY() {
        return Coords.cubeToMaxBlock(y);
    }

    public int getMaxBlockZ() {
        return Coords.cubeToMaxBlock(z);
    }

    public BlockPos getMinBlockPos() {
        return new BlockPos(
                Coords.cubeToMinBlock(x),
                Coords.cubeToMinBlock(y),
                Coords.cubeToMinBlock(z)
        );
    }

    public BlockPos getCenterBlockPos() {
        return new BlockPos(
                Coords.cubeToMinBlock(x) + 8,
                Coords.cubeToMinBlock(y) + 8,
                Coords.cubeToMinBlock(z) + 8
        );
    }

    public CubePos above() {
        return CubePos.of(x, y + 1, z);
    }

    public CubePos below() {
        return CubePos.of(x, y - 1, z);
    }

    public CubePos north() {
        return CubePos.of(x, y, z - 1);
    }

    public CubePos south() {
        return CubePos.of(x, y, z + 1);
    }

    public CubePos east() {
        return CubePos.of(x + 1, y, z);
    }

    public CubePos west() {
        return CubePos.of(x - 1, y, z);
    }

    public long asLong() {
        return (((long) x) & 0x1fffffL)
            | ((((long) y) & 0x1fffffL) << 21)
            | ((((long) z) & 0x1fffffL) << 42);
    }

    @Override
    public int compareTo(CubePos other) {
        int dy = Integer.compare(this.y, other.y);
        if (dy != 0) return dy;
        int dx = Integer.compare(this.x, other.x);
        if (dx != 0) return dx;
        return Integer.compare(this.z, other.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CubePos)) return false;
        CubePos other = (CubePos) o;
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "CubePos[" + x + ", " + y + ", " + z + "]";
    }
}
