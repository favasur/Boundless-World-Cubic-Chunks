package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.BitSet;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.NewServerHeightMap
public class NewServerHeightMap implements IHeightMap {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Int2ObjectMap<HeightMap>[] heightmapsByScale = new Int2ObjectOpenHashMap[8];

    public NewServerHeightMap() {
        for (int i = 0; i < this.heightmapsByScale.length; i++) {
            this.heightmapsByScale[i] = new Int2ObjectOpenHashMap<>();
        }
    }

    public void addCube(ICube cube) {
        // placeholder hook — scale resolution differs per cube
    }

    public void unloadCube(ICube cube) {
        // placeholder hook — opposite of addCube
    }

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        // Update the bit-mapped scale bucket this block belongs to.
        if (opacity > 0) {
            int cubeY = Coords.blockToCube(blockY);
            int localY = Coords.blockToLocal(blockY);
            int scale = chooseScale(Coords.localToBlock(cubeY, localY));
            Int2ObjectMap<HeightMap> map = this.heightmapsByScale[scale];
            map.computeIfAbsent(Coords.cubeToMinBlock(cubeY), y -> new HeightMap(scale, y));
        }
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return 0;
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        return 0;
    }

    @Override
    public int getLowestTopBlockY() {
        return 0;
    }

    private static int chooseScale(int blockY) {
        int abs = Math.abs(blockY);
        if (abs < 16) return 0;
        if (abs < 128) return 1;
        if (abs < 1024) return 2;
        if (abs < 8192) return 3;
        if (abs < 65536) return 4;
        return 7;
    }

    private static final class HeightMap {
        private final int[] heights = new int[256];
        private final BitSet invalidated = new BitSet(256);
        private final int scale;
        private final int scaledY;

        HeightMap(int scale, int scaledY) {
            this.scale = scale;
            this.scaledY = scaledY;
        }
    }

    @Override
    public boolean isEmpty(int localX, int localZ) { return false; }
}
