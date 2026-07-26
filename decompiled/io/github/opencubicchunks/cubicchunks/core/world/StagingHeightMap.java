package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class StagingHeightMap implements IHeightMap {
   private final List<ICube> stagedCubes = new ArrayList<>();
   private final int[] heightmap = new int[256];
   private final BitSet dirtyFlag = new BitSet(this.heightmap.length);

   public StagingHeightMap() {
   }

   public void addStagedCube(ICube cube) {
      this.stagedCubes.add(cube);
      this.stagedCubes.sort(Comparator.comparingInt(c -> -c.getCoords().getY()));
      if (!cube.isEmpty()) {
         this.dirtyFlag.set(0, this.heightmap.length);
      }
   }

   public void removeStagedCube(ICube cube) {
      if (this.stagedCubes.remove(cube) && !cube.isEmpty()) {
         this.dirtyFlag.set(0, this.heightmap.length);
      }
   }

   @Override
   public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
      if (opacity > 0) {
         if (blockY > this.getTopBlockY(localX, localZ)) {
            this.heightmap[this.index(localX, localZ)] = blockY;
         }
      } else if (blockY == this.getTopBlockY(localX, localZ)) {
         this.dirtyFlag.set(this.index(localX, localZ));
      }
   }

   private int index(int localX, int localZ) {
      return localZ << 4 | localX;
   }

   @Override
   public int getTopBlockY(int localX, int localZ) {
      int idx = this.index(localX, localZ);
      if (!this.dirtyFlag.get(idx)) {
         return this.heightmap[idx];
      } else {
         this.dirtyFlag.clear(idx);
         return this.heightmap[idx] = this.computeHeightMap(localX, localZ);
      }
   }

   private int computeHeightMap(int localX, int localZ) {
      int j = 0;

      for (int stagedCubesSize = this.stagedCubes.size(); j < stagedCubesSize; j++) {
         ICube stagedCube = this.stagedCubes.get(j);
         ExtendedBlockStorage ebs = stagedCube.getStorage();
         if (ebs != null && !ebs.func_76663_a()) {
            for (int i = 15; i >= 0; i--) {
               if (ebs.func_177485_a(localX, i, localZ).func_185891_c() > 0) {
                  return Coords.localToBlock(stagedCube.getY(), i);
               }
            }
         }
      }

      return -2147483616;
   }

   @Override
   public int getTopBlockYBelow(int localX, int localZ, int blockY) {
      throw new UnsupportedOperationException("Not implemented for staging heightmap");
   }

   @Override
   public int getLowestTopBlockY() {
      throw new UnsupportedOperationException("Not implemented for staging heightmap");
   }
}
