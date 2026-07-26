package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.EntityBat_AiHeightLimit
// 1.21: in cubic worlds bats can sleep at any Y; this hook fires after the engine's
// hang-target check so other AI doesn't crash with out-of-cube positions.
@Mixin(Bat.class)
public abstract class MixinEntityBatAiHeightLimit {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void cc$aiStep(CallbackInfo ci) {
        Bat bat = (Bat) (Object) this;
        Level level = bat.level();
        // If the bat's hang target is in an unloaded cube, drop it so it retargets.
        CubePos batCube = CubePos.of(bat.getBlockX() >> 4, bat.getBlockY() >> 4, bat.getBlockZ() >> 4);
        if (Math.abs(batCube.getY()) > 1500) {
            bat.discard();
        }
    }
}
