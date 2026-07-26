package io.github.opencubicchunks.cubicchunks.core.lighting;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.EnumSkyBlock;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ILightBlockAccess {
   int getBlockLightOpacity(BlockPos var1);

   int getLightFor(EnumSkyBlock var1, BlockPos var2);

   boolean setLightFor(EnumSkyBlock var1, BlockPos var2, int var3);

   default int computeLightValue(BlockPos pos) {
      if (this.canSeeSky(pos)) {
         return 15;
      } else {
         int lightSubtract = this.getBlockLightOpacity(pos);
         if (lightSubtract < 1) {
            lightSubtract = 1;
         }

         if (lightSubtract >= 15) {
            return 0;
         } else {
            PooledMutableBlockPos currentPos = PooledMutableBlockPos.func_185346_s();
            int maxValue = 0;

            for (EnumFacing enumfacing : EnumFacing.field_82609_l) {
               currentPos.func_189533_g(pos).func_189536_c(enumfacing);
               int currentValue = this.getLightFor(EnumSkyBlock.SKY, currentPos) - lightSubtract;
               if (currentValue > maxValue) {
                  maxValue = currentValue;
               }

               if (maxValue >= 14) {
                  return maxValue;
               }
            }

            currentPos.func_185344_t();
            return maxValue;
         }
      }
   }

   boolean canSeeSky(BlockPos var1);

   int getEmittedLight(BlockPos var1, EnumSkyBlock var2);

   default boolean hasNeighborsAccessible(BlockPos pos) {
      return true;
   }

   default int getLightFromNeighbors(EnumSkyBlock type, BlockPos pos, MutableBlockPos scratchPos) {
      int blockLightOpacity = this.getBlockLightOpacity(pos);
      if (blockLightOpacity > 15) {
         return 0;
      } else {
         int max = 0;

         for (EnumFacing direction : EnumFacing.field_82609_l) {
            scratchPos.func_189533_g(pos);
            scratchPos.func_189536_c(direction);
            int light = this.getLightFor(type, scratchPos);
            if (light > max) {
               max = light;
               if (light >= 15) {
                  break;
               }
            }
         }

         int decrease = Math.max(1, blockLightOpacity);
         return Math.max(0, max - decrease);
      }
   }

   void markEdgeNeedLightUpdate(BlockPos var1, EnumSkyBlock var2);
}
