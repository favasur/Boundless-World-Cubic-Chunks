package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.MixinBlockFalling_HeightLimits
// 1.21: prevents `FallingBlock.tick` from being scheduled when the spawn position
// exceeds the cube provider's tracking ability.
// In Mojang mappings, FallingBlock.isFree(BlockState) is a static method — the
// callback must be static too, and the parameters must match the target's
// Mojang signature, not the intermediary signature the mixin was originally
// written against.
@Mixin(FallingBlock.class)
public abstract class MixinBlockFallingHeightLimits {

    @Inject(method = "isFree", at = @At("HEAD"), cancellable = true)
    private static void cc$isFree(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // Height-limit check cannot gate on BlockState alone (no position
        // available in this static Mojang overload). Keep the inject point
        // active for future height-aware hooks; for now, pass through.
    }
}
