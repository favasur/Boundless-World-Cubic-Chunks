package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen.tree;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenHugeTrees;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenHugeTrees.class})
public class MixinWorldGenHugeTrees {
   public MixinWorldGenHugeTrees() {
   }

   @ModifyConstant(
      method = {"isSpaceAt"},
      constant = {@Constant(
         intValue = 1,
         ordinal = 1
      )}
   )
   private int isSpace_getMinHeight(int val, World worldIn, BlockPos leavesPos, int height) {
      return ((ICubicWorld)worldIn).getMinHeight() + 1;
   }

   @ModifyConstant(
      method = {"isSpaceAt"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int isSpace_getMaxHeight(int val, World worldIn, BlockPos leavesPos, int height) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }

   @ModifyConstant(
      method = {"isSpaceAt"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO},
         ordinal = 1
      )}
   )
   private int getMinScanHeight(int orig, World worldIn, BlockPos leavesPos, int height) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }
}
