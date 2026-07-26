package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({WorldInfo.class})
public class MixinWorldInfo implements ICubicWorldSettings {
   private boolean isCubic;

   public MixinWorldInfo() {
   }

   @Inject(
      method = {"populateFromWorldSettings"},
      at = {@At("RETURN")}
   )
   private void onConstructWithSettings(WorldSettings settings, CallbackInfo cbi) {
      this.isCubic = ((ICubicWorldSettings)settings).isCubic();
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/storage/WorldInfo;)V"},
      at = {@At("RETURN")}
   )
   private void onConstructWithSettings(WorldInfo other, CallbackInfo cbi) {
      this.isCubic = ((ICubicWorldSettings)other).isCubic();
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/nbt/NBTTagCompound;)V"},
      at = {@At("RETURN")}
   )
   private void onConstructWithSettings(NBTTagCompound tag, CallbackInfo cbi) {
      this.isCubic = tag.func_74767_n("isCubicWorld");
   }

   @Inject(
      method = {"updateTagCompound"},
      at = {@At("RETURN")}
   )
   private void onConstructWithSettings(NBTTagCompound nbt, NBTTagCompound playerNbt, CallbackInfo cbi) {
      nbt.func_74757_a("isCubicWorld", this.isCubic);
   }

   @Override
   public boolean isCubic() {
      return this.isCubic;
   }

   @Override
   public void setCubic(boolean cubic) {
      this.isCubic = cubic;
   }
}
