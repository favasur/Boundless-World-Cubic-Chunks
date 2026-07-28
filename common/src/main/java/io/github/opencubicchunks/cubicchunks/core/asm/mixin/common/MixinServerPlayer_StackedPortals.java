package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
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

    // 1.21.1: TeleportTarget.PostDimensionTransition lives at
    // net.minecraft.world.TeleportTarget$PostDimensionTransition in the Yarn mapping,
    // and the class isn't directly importable across all 1.21.x mappings. Resolve ONE
    // via reflection on first-class load and cache statically; if resolution fails,
    // stacked portals will NPE rather than silently teleporting wrong. We also cache
    // the DimensionTransition 6-arg ctor reflectively so we never have to type-name
    // PostDimensionTransition from a source file (the explicit cast Object→PostDim…
    // can't downcast through a non-importable class).
    private static final Object cc$NONE_POST_TRANSITION;
    @SuppressWarnings("unchecked")
    private static final java.lang.reflect.Constructor<DimensionTransition> cc$DIMENSION_TRANSITION_CTOR;

    static {
        Object none = null;
        java.lang.reflect.Constructor<DimensionTransition> ctor = null;
        try {
            Class<?> inner = Class.forName("net.minecraft.world.TeleportTarget$PostDimensionTransition");
            java.lang.reflect.Field f = inner.getDeclaredField("NONE");
            none = f.get(null);
            // Resolve the 6-arg ctor via Class.getConstructor — since DimensionTransition
            // is importable, javac compiles this; the inner-class arg is supplied reflectively.
            ctor = DimensionTransition.class.getConstructor(
                    ServerLevel.class, Vec3.class, Vec3.class, float.class, float.class, inner);
            CubicChunks.LOGGER.info("Stacked-portal teleport: resolved {} as NONE PostDimensionTransition", inner.getEnclosingClass().getName());
        } catch (Throwable t) {
            CubicChunks.LOGGER.error("Stacked-portal teleport: could not resolve TeleportTarget.PostDimensionTransition.NONE; stepping into a Nether/End portal will NPE.", t);
        }
        cc$NONE_POST_TRANSITION = none;
        cc$DIMENSION_TRANSITION_CTOR = ctor;
    }

    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void cc$interceptStackedPortal(DimensionTransition transition, CallbackInfoReturnable<DimensionTransition> cir) {
        if (!CubicChunksConfig.stackingDimensionsEnabled) {
            return;
        }
        // If static init couldn't resolve the DimensionTransition 6-arg ctor (PostDimensionTransition
        // class missing or renamed), the engine's vanilla changeDimension will NPE on the
        // real Nether/End transition. Fail loudly here so the player sees why the mod is
        // broken rather than getting an opaque NPE inside the teleport path.
        if (cc$DIMENSION_TRANSITION_CTOR == null || cc$NONE_POST_TRANSITION == null) {
            CubicChunks.LOGGER.warn("Stacked-portal teleport: missing PostDimensionTransition — falling back to vanilla teleport (will likely NPE).");
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
        // The ctor is invoked via reflection because PostDimensionTransition isn't importable
        // in the current Yarn mapping — see the static initializer above.
        DimensionTransition stacked = null;
        try {
            stacked = cc$DIMENSION_TRANSITION_CTOR.newInstance(
                    currentLevel,
                    new Vec3(self.getX(), targetY.doubleValue(), self.getZ()),
                    Vec3.ZERO,
                    self.getYRot(),
                    self.getXRot(),
                    cc$NONE_POST_TRANSITION);
        } catch (ReflectiveOperationException e) {
            CubicChunks.LOGGER.error("Stacked-portal teleport: failed to construct DimensionTransition", e);
        }
        if (stacked != null) {
            cir.setReturnValue(stacked);
        }
    }
}
