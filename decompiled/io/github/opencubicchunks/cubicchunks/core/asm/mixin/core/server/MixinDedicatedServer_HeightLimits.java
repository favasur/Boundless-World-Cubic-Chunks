package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.server;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({DedicatedServer.class})
public class MixinDedicatedServer_HeightLimits {
   public MixinDedicatedServer_HeightLimits() {
   }

   @ModifyConstant(
      method = {"init"},
      constant = {@Constant(
         intValue = 256
      )},
      require = 2
   )
   private int getDefaultBuildHeight(int oldValue) {
      return 2147479553;
   }
}
