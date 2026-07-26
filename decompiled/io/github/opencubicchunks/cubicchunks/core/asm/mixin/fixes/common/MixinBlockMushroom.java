package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({BlockMushroom.class})
public class MixinBlockMushroom {
   public MixinBlockMushroom() {
   }

   @ModifyConstant(
      method = {"canBlockStay"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )}
   )
   private int getMinHeight(int zero, World worldIn, BlockPos pos, IBlockState state) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"canBlockStay"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getMaxHeight(int _256, World worldIn, BlockPos pos, IBlockState state) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }
}
