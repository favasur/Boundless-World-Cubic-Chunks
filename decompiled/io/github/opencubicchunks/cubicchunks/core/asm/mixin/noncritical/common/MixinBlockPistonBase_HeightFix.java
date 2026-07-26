package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({BlockPistonBase.class})
public class MixinBlockPistonBase_HeightFix {
   public MixinBlockPistonBase_HeightFix() {
   }

   @Redirect(
      method = {"canPush"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/BlockPos;getY()I"
      )
   )
   @Group(
      min = 4,
      max = 4
   )
   private static int getBlockYRedirect(
      BlockPos pos, IBlockState blockStateIn, World worldIn, BlockPos posArg, EnumFacing facing, boolean destroyBlocks, EnumFacing p_185646_5_
   ) {
      ICubicWorld world = (ICubicWorld)worldIn;
      return pos.func_177956_o() >= world.getMinHeight() && pos.func_177956_o() < world.getMaxHeight() ? 64 : pos.func_177956_o();
   }
}
