package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldProvider.class})
public abstract class MixinWorldProvider {
   @Shadow
   protected World field_76579_a;

   public MixinWorldProvider() {
   }

   @Inject(
      method = {"getVoidFogYFactor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getVoidFogYFactor_injectReplace(CallbackInfoReturnable<Double> cir) {
      if (this.cubicWorld().isCubicWorld()) {
         cir.setReturnValue(Double.NaN);
         cir.cancel();
      }
   }

   private ICubicWorld cubicWorld() {
      return (ICubicWorld)this.field_76579_a;
   }
}
