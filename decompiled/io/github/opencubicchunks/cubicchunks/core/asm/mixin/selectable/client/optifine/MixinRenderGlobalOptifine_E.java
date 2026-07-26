package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({RenderGlobal.class})
public class MixinRenderGlobalOptifine_E {
   @Shadow
   private WorldClient field_72769_h;

   public MixinRenderGlobalOptifine_E() {
   }

   @ModifyConstant(
      method = {"setupTerrain"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   @Dynamic
   public int getMaxWorldHeight(int _256) {
      return ((IMinMaxHeight)this.field_72769_h).getMaxHeight();
   }
}
