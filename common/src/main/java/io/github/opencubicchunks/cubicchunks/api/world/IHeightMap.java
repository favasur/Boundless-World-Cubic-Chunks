package io.github.opencubicchunks.cubicchunks.api.world;

/**
 * Tracks the topmost opaque block per column for cubic worlds.
 * Equivalent to the legacy opacity index.
 */
public interface IHeightMap {
    /**
     * Notifies the height map that a block at (x, y, z) has changed opacity.
     */
    void onOpacityChange(int localX, int blockY, int localZ, int opacity);

    /**
     * Returns the highest opaque block y in the given column, or a sentinel value if none.
     */
    int getTopBlockY(int localX, int localZ);

    /**
     * Returns true if there are no opaque blocks in this column.
     */
    boolean isEmpty(int localX, int localZ);

    @Deprecated
    default int getTopBlockYBelow(int localX, int localZ, int blockY) {
        throw new UnsupportedOperationException("Not implemented for this heightmap type");
    }

    default int getLowestTopBlockY() {
        throw new UnsupportedOperationException("Not implemented for this heightmap type");
    }

    final class HeightMap {
        private final int[] data;

        public HeightMap(int[] heightmap) {
            this.data = heightmap;
        }

        public int get(int index) {
            return this.data[index] - 1;
        }

        public void set(int index, int value) {
            this.data[index] = value + 1;
        }

        public void increment(int index) {
            this.data[index]++;
        }

        public void decrement(int index) {
            this.data[index]--;
        }
    }
}
