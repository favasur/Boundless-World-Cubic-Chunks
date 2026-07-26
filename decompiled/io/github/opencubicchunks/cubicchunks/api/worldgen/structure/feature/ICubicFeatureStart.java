package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import net.minecraft.world.World;

public interface ICubicFeatureStart extends XYZAddressable {
   int getChunkPosY();

   void initCubic(World var1, int var2);

   CubePos getCubePos();

   boolean isCubic();
}
