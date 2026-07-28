package io.github.opencubicchunks.cubicchunks.core.asm.mixin.client;

import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.MixinWorldClient
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cc$init(CallbackInfo ci) {
        // For the skeleton, treat every client level as a cubic world. The dimension/type
        // decision will be driven by world data once networking is wired up.
        ICubicWorldInternal cubicWorld = (ICubicWorldInternal) this;
        cubicWorld.initCubicWorldClient();
        cubicWorld.setCubeCache(new CubeProviderClient((ClientLevel) (Object) this));
    }
}
