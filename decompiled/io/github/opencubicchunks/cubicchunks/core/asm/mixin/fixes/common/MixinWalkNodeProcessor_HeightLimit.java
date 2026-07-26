package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WalkNodeProcessor.class})
public abstract class MixinWalkNodeProcessor_HeightLimit extends NodeProcessor {
   public MixinWalkNodeProcessor_HeightLimit() {
   }

   @ModifyConstant(
      method = {"getStart"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO}
      )}
   )
   private int getMinHeight_GetStart(int originalY) {
      return ((ICubicWorld)this.field_186326_b.field_70170_p).getMinHeight() + originalY;
   }

   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/IBlockAccess;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
      )
   )
   private IBlockState getLoadedBlockState_getStart(IBlockAccess access, BlockPos pos) {
      return !this.field_186326_b.field_70170_p.func_175667_e(pos) ? Blocks.field_150357_h.func_176223_P() : access.func_180495_p(pos);
   }

   @ModifyConstant(
      method = {"getSafePoint"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO},
         ordinal = 1
      )}
   )
   private int getMinHeight_GetSafePoint(int originalY) {
      return ((ICubicWorld)this.field_186326_b.field_70170_p).getMinHeight() + originalY;
   }

   @ModifyConstant(
      method = {"getPathNodeType(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;"},
      constant = {@Constant(
         intValue = 1,
         ordinal = 0
      )}
   )
   private int getMinHeight_GetPathNodeType(int originalY, IBlockAccess blockaccessIn, int x, int y, int z) {
      return ((IMinMaxHeight)blockaccessIn).getMinHeight() + originalY;
   }
}
