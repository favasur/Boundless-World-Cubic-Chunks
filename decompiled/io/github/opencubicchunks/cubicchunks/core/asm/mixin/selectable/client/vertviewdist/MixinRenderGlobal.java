package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.vertviewdist;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {RenderGlobal.class},
   priority = 2000
)
public abstract class MixinRenderGlobal {
   @Shadow
   private ViewFrustum field_175008_n;
   @Shadow
   @Final
   private Minecraft field_72777_q;
   @Shadow
   private int field_72739_F;
   @Shadow
   private WorldClient field_72769_h;
   private int verticalRenderDistanceCubes;

   public MixinRenderGlobal() {
   }

   @Shadow
   public abstract void func_72712_a();

   @Inject(
      method = {"loadRenderers"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/RenderGlobal;renderDistanceChunks:I"
      )}
   )
   private void onUpdateRenderDistance(CallbackInfo cbi) {
      this.verticalRenderDistanceCubes = CubicChunksConfig.verticalCubeLoadDistance;
   }

   @Inject(
      method = {"setupTerrain"},
      at = {@At("HEAD")}
   )
   private void onSetupTerrain(CallbackInfo cbi) {
      if (((ICubicWorld)this.field_72769_h).isCubicWorld()) {
         if (this.field_72777_q.field_71474_y.field_151451_c == this.field_72739_F
            && CubicChunksConfig.verticalCubeLoadDistance != this.verticalRenderDistanceCubes) {
            this.func_72712_a();
         }
      }
   }

   @Redirect(
      method = {"setupTerrain"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/RenderGlobal;renderDistanceChunks:I"
      ),
      slice = @Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V"
         )
      )
   )
   private int onGetRenderDistance(RenderGlobal _this) {
      return !((ICubicWorld)this.field_72769_h).isCubicWorld()
         ? this.field_72777_q.field_71474_y.field_151451_c
         : Math.max(this.field_72777_q.field_71474_y.field_151451_c, CubicChunksConfig.verticalCubeLoadDistance);
   }
}
