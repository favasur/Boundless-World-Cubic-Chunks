package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedCubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedDimensionTeleporter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Re-routes vanilla Nether/End portal teleport into an in-place Y translation
 * when stacking is enabled and the player is leaving the overworld for one of
 * the absorbed sub-dims.
 *
 * <p>In 1.21.1 Mojang mappings, {@code ServerPlayer.changeDimension(DimensionTransition)}
 * returns {@code Entity} (not {@code DimensionTransition}), so we cancel the vanilla
 * call and manually teleport the player to the stacked Y band using
 * {@code ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)}.</p>
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer_StackedPortals {

    /**
     * In 1.21.1 Mojang: changeDimension(DimensionTransition) returns Entity.
     * We intercept at HEAD, check if stacking applies, and if so: cancel the
     * vanilla dimension change and perform an in-place Y teleport instead.
     */
    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cc$interceptStackedPortal(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (!CubicChunksConfig.stackingDimensionsEnabled) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel currentLevel = self.serverLevel();
        ResourceLocation currentDim = currentLevel.dimension().location();

        // Only intercept when leaving the vanilla overworld.
        if (!"minecraft".equals(currentDim.getNamespace()) || !"overworld".equals(currentDim.getPath())) {
            return;
        }
        ResourceLocation targetDim = transition.newLevel().dimension().location();
        if (!"minecraft".equals(targetDim.getNamespace())) {
            return;
        }

        Integer targetY;
        if ("the_nether".equals(targetDim.getPath())) {
            targetY = StackedDimensionTeleporter.applyStackedTeleport(currentLevel, self, targetDim, -80);
        } else if ("the_end".equals(targetDim.getPath())) {
            targetY = StackedDimensionTeleporter.applyStackedTeleport(currentLevel, self, targetDim, 12330);
        } else {
            return;
        }

        if (targetY == null) {
            return;
        }

        // Activate the stacked band BEFORE teleporting so cubes generate
        // immediately around the player's destination (avoids one-tick air gap).
        activateStackedBand(currentLevel, targetDim);

        // Cancel the vanilla dimension change. Instead, teleport the player
        // vertically within the same overworld ServerLevel to the stacked band.
        CubicChunks.LOGGER.info(
                "Stacked-portal teleport: redirecting {} to Y={} in {}",
                self.getName().getString(), targetY, currentDim);
        self.teleportTo(currentLevel, self.getX(), targetY.doubleValue(), self.getZ(), self.getYRot(), self.getXRot());
        cir.setReturnValue(self);
    }

    /**
     * Triggers lazy band activation: once a player uses a portal to enter a stacked
     * sub-dim, that band's cubes start generating. Without this call the player would
     * be teleported into empty air because the band never generates.
     */
    private static void activateStackedBand(ServerLevel level, ResourceLocation targetDim) {
        ICubeProvider provider = ((ICubicWorldInternal) level).getCubeCache();
        if (provider instanceof CubeProviderServer cps
                && cps.getCubeGenerator() instanceof StackedCubeGenerator stacked) {
            if ("the_nether".equals(targetDim.getPath())) {
                stacked.activateBand(StackedDimensions.NETHER_ID);
            } else if ("the_end".equals(targetDim.getPath())) {
                stacked.activateBand(StackedDimensions.END_ID);
            }
        }
    }
}
