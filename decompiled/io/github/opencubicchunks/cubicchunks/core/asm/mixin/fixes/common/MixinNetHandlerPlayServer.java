package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin({NetHandlerPlayServer.class})
public class MixinNetHandlerPlayServer {
   public MixinNetHandlerPlayServer() {
   }

   @ModifyConstant(
      method = {"isMovePlayerPacketInvalid"},
      slice = {@Slice(
         from = @At(
            value = "INVOKE:LAST",
            target = "Lnet/minecraft/network/play/client/CPacketPlayer;getY(D)D"
         ),
         to = @At(
            value = "INVOKE:LAST",
            target = "Lnet/minecraft/network/play/client/CPacketPlayer;getZ(D)D"
         )
      )},
      constant = {@Constant(
         doubleValue = 3.0E7
      )},
      require = 0
   )
   private static double getMaxY(double old) {
      return 2.147479551E9;
   }
}
