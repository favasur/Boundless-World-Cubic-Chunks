package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.ICubicChunkCache
public interface ICubicChunkCache {
    @Nullable
    Cube getCube(BlockPos pos);

    boolean isCubic();
}
