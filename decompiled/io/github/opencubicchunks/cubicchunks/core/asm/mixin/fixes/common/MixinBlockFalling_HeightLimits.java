package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({BlockFalling.class})
public abstract class MixinBlockFalling_HeightLimits extends Block {
   public MixinBlockFalling_HeightLimits(Material materialIn) {
      super(materialIn);
   }

   @ModifyConstant(
      method = {"checkFallable"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockFalling;canFallThrough(Lnet/minecraft/block/state/IBlockState;)Z"
         ),
         to = @At(
            value = "FIELD",
            target = "Lnet/minecraft/block/BlockFalling;fallInstantly:Z"
         )
      )},
      expect = 1
   )
   @Group(
      name = "checkFallable_getMinY1",
      min = 1,
      max = 1
   )
   private int checkFallable_getMinY1(int orig, World worldIn, BlockPos pos) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"checkFallable"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO}
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE:LAST",
            target = "Lnet/minecraft/block/BlockFalling;canFallThrough(Lnet/minecraft/block/state/IBlockState;)Z"
         ),
         to = @At(
            value = "INVOKE:ONE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"
         )
      )}
   )
   @Group(
      name = "checkFallable_getMinY2",
      min = 2,
      max = 2
   )
   private int checkFallable_getMinY2(int orig, World worldIn, BlockPos pos) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @Redirect(
      method = {"checkFallable"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
      ),
      slice = @Slice(
         from = @At(
            value = "CONSTANT",
            args = {"intValue=32"}
         ),
         to = @At("TAIL")
      ),
      require = 2
   )
   private IBlockState checkCanFallThroughGetBlockState(World world, BlockPos pos, World worldIn, BlockPos origPos) {
      if (pos == origPos) {
         return world.func_180495_p(pos);
      } else {
         return ((ICubicWorld)worldIn).isCubicWorld() && !world.func_175668_a(pos.func_177977_b(), false)
            ? Blocks.field_150357_h.func_176223_P()
            : world.func_180495_p(pos);
      }
   }

   @Redirect(
      method = {"checkFallable"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isAirBlock(Lnet/minecraft/util/math/BlockPos;)Z"
      ),
      require = 2
   )
   private boolean checkIsAirBlock(World worldIn, BlockPos pos) {
      return ((ICubicWorld)worldIn).isCubicWorld() && !worldIn.func_175668_a(pos.func_177977_b(), false) ? false : worldIn.func_175623_d(pos);
   }
}
