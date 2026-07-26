package io.github.opencubicchunks.cubicchunks.core.world.column;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeMap implements Iterable<Cube> {
   @Nonnull
   private final List<Cube> cubes = new ArrayList<>();
   @Nonnull
   private ExtendedBlockStorage[] toBlockTick = new ExtendedBlockStorage[0];
   private int relightCubeIdx = 0;
   private int relightCubeBlockIdx = (int)(Math.random() * 4096.0);

   public CubeMap() {
   }

   @Nullable
   public Cube remove(int cubeY) {
      int index = this.binarySearch(cubeY);
      return index < this.cubes.size() && this.cubes.get(index).getY() == cubeY ? this.cubes.remove(index) : null;
   }

   public void put(Cube cube) {
      int searchIndex = this.binarySearch(cube.getY());
      if (this.contains(cube.getY(), searchIndex)) {
         throw new IllegalArgumentException("Cube at " + cube.getY() + " already exists!");
      } else {
         this.cubes.add(searchIndex, cube);
      }
   }

   public Iterable<Cube> cubes(int startY, int endY) {
      boolean reverse = false;
      if (startY > endY) {
         int i = startY;
         startY = endY;
         endY = i;
         reverse = true;
      }

      int bottom = this.binarySearch(startY);
      int top = this.binarySearch(endY + 1);
      if (bottom < this.cubes.size() && top <= this.cubes.size()) {
         return reverse ? Lists.reverse(this.cubes.subList(bottom, top)) : this.cubes.subList(bottom, top);
      } else {
         return Collections.emptyList();
      }
   }

   private boolean contains(int cubeY, int searchIndex) {
      return searchIndex < this.cubes.size() && this.cubes.get(searchIndex).getY() == cubeY;
   }

   @Override
   public Iterator<Cube> iterator() {
      return this.cubes.iterator();
   }

   public Collection<Cube> all() {
      return this.cubes;
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public ExtendedBlockStorage[] getStoragesToTick() {
      if (!this.isToTickValid()) {
         int count = 0;

         for (Cube cube : this.cubes) {
            if (cube.getStorage() != null && cube.getTickets().shouldTick()) {
               count++;
            }
         }

         this.toBlockTick = new ExtendedBlockStorage[count];
         count = 0;

         for (Cube cubex : this.cubes) {
            if (cubex.getStorage() != null && cubex.getTickets().shouldTick()) {
               this.toBlockTick[count++] = cubex.getStorage();
            }
         }
      }

      return this.toBlockTick;
   }

   private boolean isToTickValid() {
      int index = 0;

      for (Cube cube : this.cubes) {
         if (cube.getStorage() != null && cube.getTickets().shouldTick()) {
            if (index >= this.toBlockTick.length) {
               return false;
            }

            if (this.toBlockTick[index++] != cube.getStorage()) {
               return false;
            }
         }
      }

      return index == this.toBlockTick.length;
   }

   private int binarySearch(int cubeY) {
      int start = 0;
      int end = this.cubes.size() - 1;

      while (start <= end) {
         int mid = start + end >>> 1;
         int at = this.cubes.get(mid).getY();
         if (at < cubeY) {
            start = mid + 1;
         } else {
            if (at <= cubeY) {
               return mid;
            }

            end = mid - 1;
         }
      }

      return start;
   }

   public void enqueueRelightChecks() {
      if (!this.cubes.isEmpty()) {
         int count = CubicChunksConfig.relightChecksPerTickPerColumn;

         for (int i = 0; i < count; i++) {
            if (this.relightCubeIdx >= this.cubes.size()) {
               this.relightCubeIdx = 0;
               this.relightCubeBlockIdx++;
               if (this.relightCubeBlockIdx >= 4096) {
                  this.relightCubeBlockIdx = 0;
               }
            }

            int reversedBits = Integer.reverse(this.relightCubeBlockIdx) >>> 20;

            assert reversedBits < 4096 && reversedBits >= 0;

            Cube cube = this.cubes.get(this.relightCubeIdx);
            int x = AddressTools.getLocalX(this.relightCubeBlockIdx);
            int y = AddressTools.getLocalY(this.relightCubeBlockIdx);
            int z = AddressTools.getLocalZ(this.relightCubeBlockIdx);
            BlockPos min = cube.getCoords().getMinBlockPos();
            cube.getWorld().func_175664_x(min.func_177982_a(x, y, z));
            this.relightCubeIdx++;
         }
      }
   }
}
