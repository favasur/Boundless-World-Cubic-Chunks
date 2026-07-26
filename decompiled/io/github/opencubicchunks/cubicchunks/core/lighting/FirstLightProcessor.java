package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.MathUtil;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.util.FastCubeBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.IntHash.Strategy;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FirstLightProcessor {
   private static final Strategy CUBE_Y_HASH = new Strategy() {
      public int hashCode(int e) {
         return e;
      }

      public boolean equals(int a, int b) {
         return a == b;
      }
   };
   @Nonnull
   private final MutableBlockPos mutablePos = new MutableBlockPos();
   @Nonnull
   private final ICubeProviderInternal cache;
   @Nonnull
   private final LightPropagator propagator = new LightPropagator();
   private final LightUpdateTracker tracker;

   public FirstLightProcessor(WorldServer world) {
      this.cache = (ICubeProviderInternal)world.func_72863_F();
      LightingManager lightingManager = ((ICubicWorldInternal)world).getLightingManager();
      this.tracker = lightingManager.getTracker();
   }

   public void diffuseSkylight(Cube cube) {
      if (!LightingManager.NO_SUNLIGHT_PROPAGATION) {
         FastCubeBlockAccess access = new FastCubeBlockAccess(this.cache, cube, 2);
         Iterable<? extends BlockPos> allBlocks = BlockPos.func_177975_b(
            cube.getCoords().getMinBlockPos().func_177982_a(-1, -1, -1), cube.getCoords().getMaxBlockPos().func_177982_a(1, 1, 1)
         );
         if (cube.isEmpty()) {
            List<BlockPos> positions = new ArrayList<>();

            for (BlockPos pos : allBlocks) {
               int localX = Coords.blockToLocal(pos.func_177958_n());
               int localY = Coords.blockToLocal(pos.func_177956_o());
               int localZ = Coords.blockToLocal(pos.func_177952_p());
               if (localX == 15 || localX == 0 || localY == 15 || localY == 0 || localZ == 15 || localZ == 0) {
                  positions.add(pos.func_185334_h());
               }
            }

            this.propagator.propagateLight(cube.getCoords().getCenterBlockPos(), positions, access, EnumSkyBlock.BLOCK, false, posx -> {
            });
         } else {
            this.propagator.propagateLight(cube.getCoords().getCenterBlockPos(), allBlocks, access, EnumSkyBlock.BLOCK, false, posx -> {
            });
         }

         if (cube.getWorld().field_73011_w.func_191066_m()) {
            this.propagator.propagateLight(cube.getCoords().getCenterBlockPos(), allBlocks, access, EnumSkyBlock.SKY, false, this.tracker::onUpdate);
            int[][] minBlockYArr = new int[16][16];
            int[][] maxBlockYArr = new int[16][16];
            int minBlockX = Coords.cubeToMinBlock(cube.getX());
            int maxBlockX = Coords.cubeToMaxBlock(cube.getX());
            int minBlockZ = Coords.cubeToMinBlock(cube.getZ());
            int maxBlockZ = Coords.cubeToMaxBlock(cube.getZ());
            int minMinHeight = Integer.MAX_VALUE;
            int maxMaxHeight = Integer.MIN_VALUE;

            for (int localX = 0; localX <= 15; localX++) {
               for (int localZ = 0; localZ <= 15; localZ++) {
                  Pair<Integer, Integer> minMax = getMinMaxLightUpdateY(cube, localX, localZ);
                  int min = minMax == null ? Integer.MAX_VALUE : (Integer)minMax.getLeft();
                  int max = minMax == null ? Integer.MIN_VALUE : (Integer)minMax.getRight();
                  minBlockYArr[localX][localZ] = min;
                  maxBlockYArr[localX][localZ] = max;
                  minMinHeight = Math.min(min, minMinHeight);
                  maxMaxHeight = Math.max(max, maxMaxHeight);
               }
            }

            Int2ObjectMap<FastCubeBlockAccess> blockAccessMap = new Int2ObjectOpenCustomHashMap(10, 0.75F, CUBE_Y_HASH);
            List<BlockPos> toUpdate = new ArrayList<>();
            IColumn column = cube.getColumn();

            for (ICube otherCube : column.getLoadedCubes(Coords.blockToCube(maxMaxHeight), Coords.blockToCube(minMinHeight))) {
               int minCubeBlockY = otherCube.getCoords().getMinBlockY();
               int maxCubeBlockY = otherCube.getCoords().getMaxBlockY();

               for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
                  for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                     int minBlockY = minBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
                     int maxBlockY = maxBlockYArr[blockX - minBlockX][blockZ - minBlockZ];
                     if (minBlockY <= maxBlockY
                        && MathUtil.rangesIntersect(minBlockY, maxBlockY, minCubeBlockY, maxCubeBlockY)
                        && (otherCube == cube || otherCube.isInitialLightingDone())) {
                        this.mutablePos.func_181079_c(blockX, this.mutablePos.func_177956_o(), blockZ);
                        if (!this.diffuseSkylightInBlockColumn(otherCube, this.mutablePos, minBlockY, maxBlockY, blockAccessMap, toUpdate)) {
                           throw new IllegalStateException("Check light failed at " + this.mutablePos + "!");
                        }
                     }
                  }
               }

               if (!toUpdate.isEmpty()) {
                  this.propagator
                     .propagateLight(
                        otherCube.getCoords().getCenterBlockPos(),
                        toUpdate,
                        (ILightBlockAccess)blockAccessMap.get(otherCube.getY()),
                        EnumSkyBlock.SKY,
                        this.tracker::onUpdate
                     );
                  toUpdate.clear();
               }
            }
         }
      }
   }

   private boolean diffuseSkylightInBlockColumn(
      ICube cube, MutableBlockPos pos, int minBlockY, int maxBlockY, Int2ObjectMap<FastCubeBlockAccess> blockAccessMap, List<BlockPos> posToUpdate
   ) {
      int cubeMinBlockY = Coords.cubeToMinBlock(cube.getY());
      int cubeMaxBlockY = Coords.cubeToMaxBlock(cube.getY());
      int maxBlockYInCube = Math.min(cubeMaxBlockY, maxBlockY);
      int minBlockYInCube = Math.max(cubeMinBlockY, minBlockY);
      FastCubeBlockAccess blockAccess = (FastCubeBlockAccess)blockAccessMap.get(cube.getY());
      if (blockAccess == null) {
         blockAccess = new FastCubeBlockAccess(this.cache, cube, 1);
         blockAccessMap.put(cube.getY(), blockAccess);
      }

      for (int blockY = maxBlockYInCube; blockY >= minBlockYInCube; blockY--) {
         pos.func_185336_p(blockY);
         if (needsSkylightUpdate(blockAccess, pos)) {
            posToUpdate.add(pos.func_185334_h());
         }
      }

      return true;
   }

   private static boolean needsSkylightUpdate(@Nonnull ILightBlockAccess access, @Nonnull MutableBlockPos pos) {
      if (access.getBlockLightOpacity(pos) >= 15) {
         return false;
      } else {
         int computedLight = access.computeLightValue(pos);
         if (computedLight != access.getLightFor(EnumSkyBlock.SKY, pos)) {
            return true;
         } else {
            for (EnumFacing facing : EnumFacing.values()) {
               pos.func_189536_c(facing);
               int currentLight = access.getLightFor(EnumSkyBlock.SKY, pos);
               int currentOpacity = Math.max(1, access.getBlockLightOpacity(pos));
               pos.func_189536_c(facing.func_176734_d());
               if (computedLight == currentLight - currentOpacity) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   @Nullable
   private static ImmutablePair<Integer, Integer> getMinMaxLightUpdateY(@Nonnull Cube cube, int localX, int localZ) {
      IColumn column = cube.getColumn();
      int heightMax = ((IColumnInternal)column).getHeightWithStaging(localX, localZ) - 1;
      int cubeY = cube.getY();
      if (Coords.blockToCube(heightMax) < cubeY) {
         return null;
      } else if (cubeY < Coords.blockToCube(heightMax)) {
         return null;
      } else {
         int previousMaxHeight = column.getOpacityIndex().getTopBlockY(localX, localZ);
         return new ImmutablePair(previousMaxHeight, heightMax);
      }
   }
}
