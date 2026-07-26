package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.vertviewdist;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({EntityRenderer.class})
public class MixinEntityRenderer {
   @Shadow
   @Final
   private Minecraft field_78531_r;

   public MixinEntityRenderer() {
   }

   @Redirect(
      method = {"updateRenderer", "setupCameraTransform", "renderWorldPass", "updateFogColor"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I"
      )
   )
   private int getRenderDistance(GameSettings settings) {
      return !((ICubicWorld)this.field_78531_r.field_71441_e).isCubicWorld()
         ? settings.field_151451_c
         : Math.max(settings.field_151451_c, CubicChunksConfig.verticalCubeLoadDistance);
   }
}
