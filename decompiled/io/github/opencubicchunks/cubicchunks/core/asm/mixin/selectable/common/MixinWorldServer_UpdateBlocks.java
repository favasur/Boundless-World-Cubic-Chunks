package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {WorldServer.class},
   priority = 1001
)
public abstract class MixinWorldServer_UpdateBlocks implements ICubicWorldServer {
   public MixinWorldServer_UpdateBlocks() {
   }

   @Redirect(
      method = {"updateBlocks"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/GameRules;getInt(Ljava/lang/String;)I"
      ),
      require = 1
   )
   public int redirectGetRandomTickSpeed(GameRules gameRules, String ruleName) {
      return this.isCubicWorld() ? 0 : gameRules.func_180263_c(ruleName);
   }
}
