package io.github.opencubicchunks.cubicchunks.core.world.cube;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.cube.CubeSplitTickSet
// 1.21: vanilla keeps a "always-tick" set per chunk section. Cubic chunks extend
// it across cube Y-bands. Storage strategy: a HashSet per cube populated lazily.
public class CubeSplitTickSet {

    private final Set<BlockPos> ticking = new HashSet<>();

    public void add(BlockPos pos) {
        ticking.add(pos.immutable());
    }

    public boolean remove(BlockPos pos) {
        return ticking.remove(pos);
    }

    public boolean contains(BlockPos pos) {
        return ticking.contains(pos);
    }

    public int size() {
        return ticking.size();
    }

    public void clear() {
        ticking.clear();
    }
}
