package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({BlockStaticLiquid.class})
public class MixinBlockStaticLiquid {
   public MixinBlockStaticLiquid() {
   }

   @ModifyConstant(
      method = {"updateTick"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )}
   )
   private int getMinHeightTick(int zero, World worldIn, BlockPos pos, IBlockState state, Random rand) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"updateTick"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getMaxHeightTick(int _256, World worldIn, BlockPos pos, IBlockState state, Random rand) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }

   @ModifyConstant(
      method = {"getCanBlockBurn"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )}
   )
   private int getMinHeightBurn(int zero, World worldIn, BlockPos pos) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"getCanBlockBurn"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getMaxHeightBurn(int _256, World worldIn, BlockPos pos) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }
}
