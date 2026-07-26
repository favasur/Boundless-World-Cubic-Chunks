package io.github.opencubicchunks.cubicchunks.api.util;

/**
 * Utility methods for converting between block, cube, and local coordinates.
 */
public final class Coords {
    public static final int CUBE_SIZE = 16;
    public static final int BIOME_SIZE = 4;

    private Coords() {
        throw new AssertionError("Utility class");
    }

    public static int blockToLocal(int block) {
        return block & 0xF;
    }

    public static int blockToCube(int block) {
        return block >> 4;
    }

    public static int cubeToMinBlock(int cube) {
        return cube << 4;
    }

    public static int cubeToMaxBlock(int cube) {
        return (cube << 4) + 15;
    }

    public static int localToBlock(int cube, int local) {
        return (cube << 4) + local;
    }

    public static int blockToBiome(int block) {
        return block >> 2;
    }
}
