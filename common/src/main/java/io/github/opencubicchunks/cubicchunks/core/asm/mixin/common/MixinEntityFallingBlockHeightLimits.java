package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.MixinEntityFallingBlock_HeightLimits
// 1.21: falling blocks can leave a cube before finishing fall. Avoid letting them
// queue new BlockPos across cube-band boundaries and skipping the unload path.
@Mixin(FallingBlockEntity.class)
public abstract class MixinEntityFallingBlockHeightLimits {

    @Inject(method = "tick", at = @At("HEAD"))
    private void cc$tick(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        Level level = self.level();
        if (!((ICubicWorldInternal) level).isCubicWorld()) {
            return;
        }
        BlockPos pos = self.blockPosition();
        // When the falling block is in an unloaded cube band, drop it to avoid stale physics state.
        if (((ICubicWorldInternal) level).getCubeCache().getCube(
                Coords.blockToCube(pos.getX()),
                Coords.blockToCube(pos.getY()),
                Coords.blockToCube(pos.getZ())) == null) {
            self.discard();
            ci.cancel();
        }
    }

    @Inject(method = "getSpawnDelay", at = @At("HEAD"), cancellable = true)
    private void cc$getSpawnDelay(CallbackInfoReturnable<Integer> cir) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (((ICubicWorldInternal) self.level()).isCubicWorld()) {
            cir.setReturnValue(Math.min(cir.getReturnValue(), 1));
        }
    }
}
