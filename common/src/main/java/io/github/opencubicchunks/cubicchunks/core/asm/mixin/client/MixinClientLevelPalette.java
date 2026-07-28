package io.github.opencubicchunks.cubicchunks.core.asm.mixin.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Client-side palette dispatch: swaps sky / cloud / horizon colors per stacked
 * sub-dim band based on the camera's cubeY. 1.21 port: cast
 * {@code cameraEntity.getY()} (which now returns {@code double}) to {@code int}
 * before passing into {@code Coords.blockToCube}.
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevelPalette {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void cc$overrideSkyColor(Vec3 cameraPos, float temperature, CallbackInfoReturnable<Vec3> cir) {
        Optional<StackedDimension> active = activeBandFromCamera((ClientLevel) (Object) this);
        if (active.isEmpty()) return;
        cir.setReturnValue(paletteVec3(active.get().palette().skyColorRgb()));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void cc$overrideCloudColor(float temperature, CallbackInfoReturnable<Vec3> cir) {
        Optional<StackedDimension> active = activeBandFromCamera((ClientLevel) (Object) this);
        if (active.isEmpty()) return;
        cir.setReturnValue(paletteVec3(active.get().palette().skyColorRgb()));
    }

    @Unique
    private static Optional<StackedDimension> activeBandFromCamera(ClientLevel clientLevel) {
        if (clientLevel == null) return Optional.empty();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return Optional.empty();
        Entity cameraEntity = cameraEntity(mc);
        if (cameraEntity == null || cameraEntity.level() != clientLevel) return Optional.empty();
        int cubeY = Coords.blockToCube((int) cameraEntity.getY());
        Optional<StackedDimension> direct = StackedDimensionRegistry.findForCubeY(cubeY);
        if (direct.isPresent() && !direct.get().id().equals(StackedDimensions.OVERWORLD_ID)) {
            return direct;
        }
        return Optional.empty();
    }

    @Unique
    @Nullable
    private static Entity cameraEntity(Minecraft mc) {
        try {
            Camera camera = mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
            if (camera != null) {
                Entity e = camera.getEntity();
                if (e != null) return e;
            }
        } catch (Throwable ignored) {
            // gameRenderer not initialised yet; fall back below.
        }
        return mc.player;
    }

    @Unique
    private static Vec3 paletteVec3(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        return new Vec3(r, g, b);
    }
}
