package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({NetHandlerPlayServer.class})
public interface INetHandlerPlayServer {
   @Accessor
   int getTeleportId();

   @Accessor
   void setTeleportId(int var1);
}
