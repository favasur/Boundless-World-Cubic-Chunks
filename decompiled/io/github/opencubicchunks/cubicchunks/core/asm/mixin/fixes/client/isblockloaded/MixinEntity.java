package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.client.isblockloaded;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({Entity.class})
public abstract class MixinEntity {
   @Shadow
   public double field_70163_u;

   public MixinEntity() {
   }

   @Shadow
   public abstract float func_70047_e();

   @ModifyArg(
      method = {"getBrightnessForRender"},
      index = 1,
      at = @At(
         target = "Lnet/minecraft/util/math/BlockPos$MutableBlockPos;<init>(III)V",
         value = "INVOKE"
      )
   )
   public int getModifiedYPos_getBrightnessForRender(int y) {
      return MathHelper.func_76128_c(this.field_70163_u + (double)this.func_70047_e());
   }
}
