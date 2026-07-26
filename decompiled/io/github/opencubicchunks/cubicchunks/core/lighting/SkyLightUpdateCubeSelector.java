package io.github.opencubicchunks.cubicchunks.core.lighting;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class SkyLightUpdateCubeSelector {
   private SkyLightUpdateCubeSelector() {
      throw new RuntimeException();
   }

   static TIntSet getCubesY(Chunk column, int localX, int localZ, int minBlockY, int maxBlockY) {
      World world = column.func_177412_p();
      TIntSet cubesToDiffuse = new TIntHashSet();
      if (!world.field_73011_w.func_191066_m()) {
         return cubesToDiffuse;
      } else {
         MutableBlockPos blockPos = new MutableBlockPos(
            Coords.localToBlock(column.field_76635_g, localX), maxBlockY - 1, Coords.localToBlock(column.field_76647_h, localZ)
         );
         int newMaxBlockY = column.func_76611_b(localX, localZ) - 1;
         int maxCubeY = Coords.blockToCube(newMaxBlockY);

         for (ICube cube : ((IColumn)column).getLoadedCubes()) {
            int cubeY = cube.getY();
            int minCubeBlockY = cubeY * 16;
            if (maxBlockY >= minCubeBlockY) {
               if (cubeY > maxCubeY) {
                  blockPos.func_181079_c(Coords.localToBlock(cube.getX(), localX), Coords.cubeToMinBlock(cubeY), Coords.localToBlock(cube.getZ(), localZ));
                  if (cube.getLightFor(EnumSkyBlock.SKY, blockPos) != 15) {
                     cubesToDiffuse.add(cube.getY());
                  }
               } else if (cubeY == maxCubeY) {
                  cubesToDiffuse.add(cube.getY());
                  if ((cube = ((IColumn)column).getLoadedCube(maxCubeY - 1)) != null) {
                     cubesToDiffuse.add(cube.getY());
                  }
               } else if (cubeY != maxCubeY - 1) {
                  assert cubeY < maxCubeY - 1;

                  blockPos.func_181079_c(Coords.localToBlock(cube.getX(), localX), Coords.cubeToMaxBlock(cubeY), Coords.localToBlock(cube.getZ(), localZ));
                  if (minCubeBlockY + 15 >= minBlockY && cube.getLightFor(EnumSkyBlock.SKY, blockPos) != 0) {
                     cubesToDiffuse.add(cube.getY());
                  }
               }
            }
         }

         return cubesToDiffuse;
      }
   }
}
