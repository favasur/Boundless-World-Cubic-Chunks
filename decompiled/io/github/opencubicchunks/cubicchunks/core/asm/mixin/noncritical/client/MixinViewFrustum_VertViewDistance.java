package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinConfig;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ViewFrustum.class})
public class MixinViewFrustum_VertViewDistance {
   @Shadow
   @Final
   protected World field_178167_b;
   @Shadow
   @Final
   protected RenderGlobal field_178169_a;
   private int renderDistance = 16;

   public MixinViewFrustum_VertViewDistance() {
   }

   @Inject(
      method = {"setCountChunksXYZ"},
      at = {@At("HEAD")}
   )
   private void onSetCountChunks(int renderDistance, CallbackInfo cbi) {
      if (((ICubicWorld)this.field_178167_b).isCubicWorld()) {
         this.renderDistance = (
                  CubicChunksMixinConfig.BoolOptions.VERT_RENDER_DISTANCE.getValue() ? CubicChunksConfig.verticalCubeLoadDistance : renderDistance
               )
               * 2
            + 1;
      } else {
         this.renderDistance = 16;
      }
   }

   @ModifyConstant(
      method = {"setCountChunksXYZ"},
      constant = {@Constant(
         intValue = 16
      )}
   )
   private int getYViewDistance(int oldDistance) {
      return this.renderDistance;
   }
}
