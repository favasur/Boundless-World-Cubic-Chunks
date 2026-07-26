package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({BlockLilyPad.class})
public abstract class MixinBlockLilyPad_HeightLimits extends BlockBush {
   public MixinBlockLilyPad_HeightLimits(Material materialIn) {
      super(materialIn);
   }

   @ModifyConstant(
      method = {"canBlockStay"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      expect = 1
   )
   private int canBlockStay_getMinY(int orig, World worldIn, BlockPos pos, IBlockState state) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"canBlockStay"},
      constant = {@Constant(
         intValue = 256
      )},
      expect = 1
   )
   private int canBlockStay_getMaxY(int orig, World worldIn, BlockPos pos, IBlockState state) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }
}
