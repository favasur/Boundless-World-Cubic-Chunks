package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedDimensionTeleporter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Re-routes vanilla Nether/End portal teleport into an in-place Y translation
 * when stacking is enabled and the player is leaving the overworld for one of
 * the absorbed sub-dims. 1.21 port: DimensionTransition is at
 * {@code net.minecraft.world.level.portal.DimensionTransition}; constructor
 * signature in 1.21.1 is
 * {@code (ServerLevel, Vec3, Vec3, float, float)}.
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer_StackedPortals {

    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void cc$interceptStackedPortal(DimensionTransition transition, CallbackInfoReturnable<DimensionTransition> cir) {
        if (!CubicChunksConfig.stackingDimensionsEnabled) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel currentLevel = self.serverLevel();
        ResourceLocation currentDim = currentLevel.dimension().location();
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
        // Same ServerLevel teleport, new Y inside the stacked band. Engine sees a
        // valid DimensionTransition for a level the player is already on, so it
        // performs an in-server teleport (no real dimension change).
        // 1.21.x: use the canonical 4-arg ctor (ServerLevel, Vec3, float, float). The engine
        // interprets an in-place teleport when newLevel() matches currentLevel().
        // 1.21.1: DimensionTransition ctors are (ServerLevel, Vec3, Vec3, float, float, PostDimensionTransition)
        // and (ServerLevel, Entity, PostDimensionTransition). Use the 6-arg form with zero velocity
        // and PostDimensionTransition.NONE so the engine performs an in-place teleport.
        DimensionTransition stacked = new DimensionTransition(
                currentLevel,
                new Vec3(self.getX(), targetY.doubleValue(), self.getZ()),
                Vec3.ZERO,
                self.getYRot(),
                self.getXRot(),
                null);
        cir.setReturnValue(stacked);
    }
}
