package io.github.opencubicchunks.cubicchunks.api.worldgen.structure;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import java.util.Random;
import net.minecraft.world.World;

public interface ICubicStructureGenerator {
   void generate(World var1, CubePrimer var2, CubePos var3);

   default void generate(
      World world, CubePrimer cube, CubePos cubePos, ICubicStructureGenerator.Handler handler, int range, int rangeY, int spacingBitCount, int spacingBitCountY
   ) {
      Random rand = new Random(world.func_72905_C());
      long randXMul = rand.nextLong();
      long randYMul = rand.nextLong();
      long randZMul = rand.nextLong();
      int spacing = 1 << spacingBitCount;
      int spacingBits = spacing - 1;
      int spacingY = 1 << spacingBitCountY;
      int spacingBitsY = spacingY - 1;
      int radius = range | spacingBits;
      int radiusY = rangeY | spacingBitsY;
      int cubeXOriginBase = cubePos.getX() | spacingBits;
      int cubeYOriginBase = cubePos.getY() | spacingBitsY;
      int cubeZOriginBase = cubePos.getZ() | spacingBits;
      long randSeed = world.func_72905_C();

      for (int xOrigin = cubeXOriginBase - radius; xOrigin <= cubeXOriginBase + radius; xOrigin += spacing) {
         long randX = (long)xOrigin * randXMul ^ randSeed;

         for (int yOrigin = cubeYOriginBase - radiusY; yOrigin <= cubeYOriginBase + radiusY; yOrigin += spacingY) {
            long randY = (long)yOrigin * randYMul ^ randX;

            for (int zOrigin = cubeZOriginBase - radius; zOrigin <= cubeZOriginBase + radius; zOrigin += spacing) {
               long randZ = (long)zOrigin * randZMul ^ randY;
               rand.setSeed(randZ);
               handler.generate(world, rand, cube, xOrigin, yOrigin, zOrigin, cubePos);
            }
         }
      }
   }

   @FunctionalInterface
   public interface Handler {
      void generate(World var1, Random var2, CubePrimer var3, int var4, int var5, int var6, CubePos var7);
   }
}
