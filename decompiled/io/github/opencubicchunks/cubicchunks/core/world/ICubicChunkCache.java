package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;

public interface ICubicChunkCache {
   @Nullable
   Cube getCube(BlockPos var1);

   boolean isCubic();
}
