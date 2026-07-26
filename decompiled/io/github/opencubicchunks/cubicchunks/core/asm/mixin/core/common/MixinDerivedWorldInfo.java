package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({DerivedWorldInfo.class})
public class MixinDerivedWorldInfo extends MixinWorldInfo {
   @Shadow
   @Final
   private WorldInfo field_76115_a;

   public MixinDerivedWorldInfo() {
   }

   @Override
   public boolean isCubic() {
      return ((ICubicWorldSettings)this.field_76115_a).isCubic();
   }
}
