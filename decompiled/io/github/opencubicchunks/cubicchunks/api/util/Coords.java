package io.github.opencubicchunks.cubicchunks.api.util;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Coords {
   public static final int NO_HEIGHT = -2147483616;
   public static final int BIOMES_PER_CUBE = 64;

   public Coords() {
   }

   public static BlockPos midPos(BlockPos p1, BlockPos p2) {
      return new BlockPos(
         (p1.func_177958_n() >> 1) + (p2.func_177958_n() >> 1) + (p1.func_177958_n() & p2.func_177958_n() & 1),
         (p1.func_177956_o() >> 1) + (p2.func_177956_o() >> 1) + (p1.func_177956_o() & p2.func_177956_o() & 1),
         (p1.func_177952_p() >> 1) + (p2.func_177952_p() >> 1) + (p1.func_177952_p() & p2.func_177952_p() & 1)
      );
   }

   public static int blockToLocal(int val) {
      return val & 15;
   }

   public static int blockToCube(int val) {
      return val >> 4;
   }

   public static int blockCeilToCube(int val) {
      return -(-val >> 4);
   }

   public static int blockToBiome(int val) {
      return (val & 14) >> 1;
   }

   public static int localToBlock(int cubeVal, int localVal) {
      return cubeToMinBlock(cubeVal) + localVal;
   }

   public static int cubeToMinBlock(int val) {
      return val << 4;
   }

   public static int cubeToMaxBlock(int val) {
      return cubeToMinBlock(val) + 15;
   }

   public static int getCubeXForEntity(Entity entity) {
      return blockToCube(MathHelper.func_76128_c(entity.field_70165_t));
   }

   public static int getCubeZForEntity(Entity entity) {
      return blockToCube(MathHelper.func_76128_c(entity.field_70161_v));
   }

   public static int getCubeYForEntity(Entity entity) {
      return blockToCube(MathHelper.func_76128_c(entity.field_70163_u));
   }

   public static BlockPos getCubeCenter(ICube cube) {
      return new BlockPos(cubeToMinBlock(cube.getX()) + 8, cubeToMinBlock(cube.getY()) + 8, cubeToMinBlock(cube.getZ()) + 8);
   }

   public static int blockToCube(double blockPos) {
      return blockToCube(MathHelper.func_76128_c(blockPos));
   }

   public static int cubeToCenterBlock(int cubeVal) {
      return localToBlock(cubeVal, 8);
   }

   public static int getMinCubePopulationPos(int coord) {
      return localToBlock(blockToCube(coord - 8), 8);
   }

   public static long coordsSeedHash(long seed, int x, int y, int z) {
      long hash = 3L;
      hash = 41L * hash + seed;
      hash = 41L * hash + (long)x;
      hash = 41L * hash + (long)y;
      return 41L * hash + (long)z;
   }

   public static Random coordsSeedRandom(long seed, int x, int y, int z) {
      return new Random(coordsSeedHash(seed, x, y, z));
   }
}
