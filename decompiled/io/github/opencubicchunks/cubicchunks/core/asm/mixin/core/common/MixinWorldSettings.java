package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({WorldSettings.class})
public class MixinWorldSettings implements ICubicWorldSettings {
   private boolean isCubic;

   public MixinWorldSettings() {
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/storage/WorldInfo;)V"},
      at = {@At("RETURN")}
   )
   private void onConstruct(WorldInfo info, CallbackInfo cbi) {
      this.isCubic = ((ICubicWorldSettings)info).isCubic();
   }

   @Inject(
      method = {"<init>(JLnet/minecraft/world/GameType;ZZLnet/minecraft/world/WorldType;)V"},
      at = {@At("RETURN")}
   )
   private void onConstruct(long seedIn, GameType gameType, boolean enableMapFeatures, boolean hardcoreMode, WorldType worldTypeIn, CallbackInfo ci) {
      this.isCubic = CubicChunksConfig.forceLoadCubicChunks != CubicChunksConfig.ForceCCMode.NONE;
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
