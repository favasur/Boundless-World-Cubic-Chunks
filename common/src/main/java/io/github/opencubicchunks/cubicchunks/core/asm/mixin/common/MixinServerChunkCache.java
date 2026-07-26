package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

// @Original: 1.12.2: N/A
@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache {

    @Shadow
    protected ServerLevel level;

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At("RETURN"))
    private void cc$tick(BooleanSupplier hasMoreTime, boolean tickChunks, CallbackInfo ci) {
        if (((ICubicWorldInternal) this.level).isCubicWorld()) {
            ICubeProvider provider = ((ICubicWorldInternal) this.level).getCubeCache();
            if (provider != null) {
                provider.tick(hasMoreTime);
            }
        }
    }
}
