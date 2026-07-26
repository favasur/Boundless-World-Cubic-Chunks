package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.BitSet;
import net.minecraft.util.BitArray;

public class NewServerHeightMap implements IHeightMap {
   private Int2ObjectMap<NewServerHeightMap.HeightMap>[] heightmapsByScale = new Int2ObjectOpenHashMap[8];

   public NewServerHeightMap() {
      for (int i = 0; i < this.heightmapsByScale.length; i++) {
         this.heightmapsByScale[i] = new Int2ObjectOpenHashMap();
      }
   }

   public void addCube(ICube cube) {
   }

   public void unloadCube(ICube cube) {
   }

   @Override
   public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
      int cubeY = Coords.blockToCube(blockY);
      if (opacity > 0) {
         int var6 = Coords.blockToLocal(blockY);
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

   private class HeightMap {
      private final BitArray heights;
      private final BitSet invalidatedPositions;
      private final int scale;
      private final int scaledY;

      private HeightMap(int scale, int scaledY) {
         this.heights = new BitArray(5 + scale * 4, 256);
         this.invalidatedPositions = new BitSet(256);
         this.scale = scale;
         this.scaledY = scaledY;
      }
   }
}
