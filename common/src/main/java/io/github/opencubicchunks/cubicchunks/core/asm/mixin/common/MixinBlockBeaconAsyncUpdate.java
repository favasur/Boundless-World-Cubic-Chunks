package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.MixinBlockBeaconAsyncUpdate
// 1.21: Beacon.asyncUpdate originally capped its scan at world height. With cubic worlds,
// the scan can span a Y range orders of magnitude larger, killing tick budgets. We bound
// the scan to a single cube band around the beacon.
@Mixin(BeaconBlockEntity.class)
public abstract class MixinBlockBeaconAsyncUpdate {

    @Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
    private static void cc$applyEffects(Level level, BlockPos pos, int beaconLevel,
                                         Holder<MobEffect> primary, Holder<MobEffect> secondary,
                                         CallbackInfo ci) {
        if (!((ICubicWorldInternal) level).isCubicWorld()) {
            return;
        }
        CubePos cubePos = CubePos.of(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        int minY = cubePos.getMinBlockY();
        int maxY = cubePos.getMaxBlockY();
        if (pos.getY() < minY || pos.getY() > maxY) {
            ci.cancel();
        }
    }

    @Inject(method = "onDataChanged", at = @At("HEAD"), cancellable = true)
    private void cc$onDataChanged(CallbackInfo ci) {
        BeaconBlockEntity self = (BeaconBlockEntity) (Object) this;
        if (Math.abs(self.getBlockPos().getY()) > 30_000) {
            ci.cancel();
        }
    }

    @Inject(method = "calculateEffects", at = @At("HEAD"), cancellable = true)
    private void cc$calculateEffects(AABB box, CallbackInfo ci) {
        BeaconBlockEntity self = (BeaconBlockEntity) (Object) this;
        if (self.getLevel() == null) {
            ci.cancel();
        }
    }
}
