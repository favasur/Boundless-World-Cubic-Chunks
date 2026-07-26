package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({EntityLivingBase.class})
public abstract class MixinEntityLivingBase extends Entity {
   public MixinEntityLivingBase(World worldIn) {
      super(worldIn);
   }

   @ModifyArg(
      method = {"travel"},
      index = 1,
      at = @At(
         target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;setPos(DDD)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;",
         value = "INVOKE",
         ordinal = 1
      )
   )
   public double moveEntityWithHeading_getReplacedY(double y) {
      return this.field_70163_u;
   }

   @ModifyConstant(
      method = {"attemptTeleport"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO}
      )}
   )
   private int getMinHeight(int orig) {
      return ((ICubicWorld)this.field_70170_p).getMinHeight();
   }

   @Redirect(
      method = {"attemptTeleport"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/BlockPos;getY()I"
      )
   )
   private int isBlockLoadedTeleportCheck(BlockPos blockPos) {
      return this.field_70170_p.func_175667_e(blockPos.func_177977_b()) ? blockPos.func_177956_o() : -2147483647;
   }
}
