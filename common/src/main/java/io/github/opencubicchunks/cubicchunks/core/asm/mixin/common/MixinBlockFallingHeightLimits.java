package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.MixinBlockFalling_HeightLimits
// 1.21: prevents `FallingBlock.tick` from being scheduled when the spawn position
// exceeds the cube provider's tracking ability.
@Mixin(FallingBlock.class)
public abstract class MixinBlockFallingHeightLimits {

    @Inject(method = "isFree", at = @At("HEAD"), cancellable = true)
    private void cc$isFree(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Math.abs(pos.getY()) > 30_000) {
            cir.setReturnValue(false);
        }
    }
}
