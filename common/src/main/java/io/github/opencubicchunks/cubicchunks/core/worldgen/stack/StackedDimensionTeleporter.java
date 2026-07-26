package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Translates a portal-style teleport into a Y-axis teleport across the stacked
 * sub-dim bands. When stacking is enabled, vanilla 'Nether Portal' and 'End Portal'
 * activations should NOT transfer the player to the {@code minecraft:the_nether} or
 * {@code minecraft:the_end} ServerLevels; instead they move the player vertically
 * inside the overworld's ServerLevel, which already contains the nether / end stacked
 * bands.
 *
 * <p>The translation is one-shot: {@link #applyStackedTeleport} computes the target Y
 * for the destination stacked dim and emits a relative teleport on the same
 * ServerLevel. Color / fog / ambient changes are surfaced through the dim's palette
 * and consumed by mixins listening on the player's current Y.</p>
 */
public final class StackedDimensionTeleporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackedDimensionTeleporter.class);

    private StackedDimensionTeleporter() {
    }

    /**
     * Vanilla resource key for the Nether dimension.
     */
    public static final ResourceLocation VANILLA_NETHER =
            ResourceLocation.withDefaultNamespace("the_nether");

    /**
     * Vanilla resource key for the End dimension.
     */
    public static final ResourceLocation VANILLA_END =
            ResourceLocation.withDefaultNamespace("the_end");

    /**
     * Map a vanilla destination dimension resource key onto the matching stacked
     * sub-dim id, or empty if the destination is not a stacked sub-dim.
     */
    public static Optional<StackedDimension> resolveDestination(ResourceLocation vanillaTargetDim) {
        if (vanillaTargetDim == null) {
            return Optional.empty();
        }
        if (vanillaTargetDim.equals(VANILLA_NETHER)) {
            return StackedDimensionRegistry.get(StackedDimensions.NETHER_ID);
        }
        if (vanillaTargetDim.equals(VANILLA_END)) {
            return StackedDimensionRegistry.get(StackedDimensions.END_ID);
        }
        return Optional.empty();
    }

    /**
     * Compute the target Y coordinate for the given {@code (dim, pos, destCenterY)}
     * combination. {@code pos} is the player's exit-side block, {@code destCenterY} is a
     * caller-provided target Y that the destination band uses as a "land at this band
     * center" hint (Nether: ~-80, End: ~520). The output is the safest block Y inside
     * the destination band for the player to spawn at.
     */
    public static int teleportTargetY(StackedDimension dest, int destCenterY) {
        int min = dest.minBlockY();
        int max = dest.maxBlockY();
        if (destCenterY >= min && destCenterY <= max) {
            return destCenterY;
        }
        // Snap to the closest in-band Y.
        if (destCenterY < min) {
            return min + 1;
        }
        return max - 1;
    }

    /**
     * Apply the stacked teleport to the player. Returns the target Y coordinate the
     * player was moved to, or null if the destination was not a stacked sub-dim.
     *
     * <p>This call does NOT move the player by itself; it returns the target Y so
     * hooks higher in the stack (the portal-teleport mixin) can call
     * {@code player.connection.teleport(x, targetY, z, yaw, pitch)} on the same
     * ServerLevel. We log the resolved target for trace visibility.</p>
     */
    @Nullable
    public static Integer applyStackedTeleport(
            ServerLevel currentLevel,
            ServerPlayer player,
            ResourceLocation vanillaTargetDim,
            int destCenterY) {
        Optional<StackedDimension> dest = resolveDestination(vanillaTargetDim);
        if (dest.isEmpty()) {
            return null;
        }
        // Only intercept if we are leaving the overworld for a stacked sub-dim.
        if (!currentLevel.dimension().location().equals(StackedDimensions.OVERWORLD_ID)
                && !currentLevel.dimension().location().getNamespace().equals("minecraft")) {
            return null;
        }
        int targetY = teleportTargetY(dest.get(), destCenterY);
        BlockPos pos = player.blockPosition();
        LOGGER.info("Stacked teleport: player {} in dim {} -> stacked sub-dim {} at y={} (anchor x={} z={})",
                player.getName().getString(),
                currentLevel.dimension().location(),
                dest.get().id(), targetY, pos.getX(), pos.getZ());
        return targetY;
    }

    /**
     * Returns the current stacked sub-dim for {@code entity}, or empty if the entity
     * is not in a registered stacked Y range.
     */
    public static Optional<StackedDimension> currentSubDim(Entity entity) {
        if (entity == null || entity.level() == null) {
            return Optional.empty();
        }
        return StackedDimensionRegistry.findForCubeY(Coords.blockToCube((int) entity.getY()));
    }

    /**
     * Test helper exposed for unit-level parity with the previous StackedDimension
     * teleport behaviors.
     */
    public static Vec3 teleportVec(StackedDimension dest, Vec3 origin, int destCenterY) {
        return new Vec3(origin.x, teleportTargetY(dest, destCenterY), origin.z);
    }
}
