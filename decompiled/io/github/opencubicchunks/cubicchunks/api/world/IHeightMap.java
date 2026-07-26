package io.github.opencubicchunks.cubicchunks.api.world;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IHeightMap {
   void onOpacityChange(int var1, int var2, int var3, int var4);

   default boolean isOccluded(int localX, int blockY, int localZ) {
      return blockY <= this.getTopBlockY(localX, localZ);
   }

   int getTopBlockY(int var1, int var2);

   @Deprecated
   int getTopBlockYBelow(int var1, int var2, int var3);

   int getLowestTopBlockY();

   public static final class HeightMap {
      private int[] data;

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
