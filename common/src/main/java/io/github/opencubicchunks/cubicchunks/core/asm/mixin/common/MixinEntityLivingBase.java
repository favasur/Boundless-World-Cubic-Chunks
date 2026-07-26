package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.MixinEntityLivingBase
// 1.21: bouncing FallingBlock / Mob-base logic uses the entity's level.getBlockState;
// if the cube is unloaded, avoid spawning harmful mobs.
@Mixin(LivingEntity.class)
public abstract class MixinEntityLivingBase {

    @Inject(method = "tick", at = @At("HEAD"))
    private void cc$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Level level = entity.level();
        if (entity.getY() > 30_000 || entity.getY() < -30_000) {
            entity.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        }
    }

    @Inject(method = "jumpInLiquid", at = @At("HEAD"), cancellable = true)
    private void cc$jumpInLiquid(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getY() > 30_000 || self.getY() < -30_000) {
            ci.cancel();
        }
    }

    @Inject(method = "checkFallDeath", at = @At("HEAD"), cancellable = true)
    private void cc$checkFallDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (Math.abs(self.getY()) > 30_000) {
            ci.cancel();
        }
    }
}
