package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client.MixinGuiOverlayDebug
// 1.21: displays a one-line "Cubic chunks: cubic world / loaded cubes: N" overlay.
@Mixin(DebugScreenOverlay.class)
public abstract class MixinGuiOverlayDebug {

    @Inject(method = "render", at = @At("HEAD"))
    private void cc$render(net.minecraft.client.gui.GuiGraphics gui, CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        if (((ICubicWorld) level).isCubicWorld()) {
            String text = "Cubic chunks: cubic world";
            gui.drawString(Minecraft.getInstance().font, text, 4, 4, 0xFFFFFF);
        }
    }
}
