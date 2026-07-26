package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.server;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinPlayerList;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DedicatedPlayerList.class})
public class MixinDedicatedPlayerList extends MixinPlayerList {
   public MixinDedicatedPlayerList() {
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void setVerticalViewDistance(DedicatedServer server, CallbackInfo cbi) {
      this.setVerticalViewDistance(server.func_71327_a("vertical-view-distance", -1));
   }
}
