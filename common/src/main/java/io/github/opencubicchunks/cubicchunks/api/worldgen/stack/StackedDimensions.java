package io.github.opencubicchunks.cubicchunks.api.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import net.minecraft.resources.ResourceLocation;

/**
 * Default stacked sub-dim registry: the three Y-bands ship with the overworld file so
 * that the vanilla Nether and The End are absorbed into a single overworld save. The
 * Y bands picked here are intentionally chosen so they do NOT overlap with the
 * overworld's vanilla build height window ({@code [-64, 320]}). Loaders may override
 * these via the registry.
 *
 * <ul>
 *     <li><b>OVERWORLD</b>: [-64, 320] — vanilla overworld Y window, dispatched when
 *         the cube Y is in the same band as the active level's vanilla column slice.</li>
 *     <li><b>NETHER</b>: [-192, -65] — sits strictly below the overworld's bedrock
 *         floor at Y=-64 so the two bands do not overlap. Range height 128 blocks,
 *         matching vanilla Nether so per-band structures (lava lakes, glowstone blobs)
 *         have the same vertical room as the standalone dimension.</li>
 *     <li><b>END</b>: [12320, 12832] — sits ~12,000 blocks above the overworld's
 *         build-height top (Y=320). Range height 512 blocks so per-band structures
 *         (obsidian pillars, end islands) breathe. Above the END band only air, between
 *         END and Overworld only air, between Overworld and Nether only air — see
 *         {@link io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedCubeGenerator}.</li>
 * </ul>
 *
 * These are {@code public static final} so callers can reference them without going
 * through a registry, but {@link StackedDimensionRegistry} is the entry point for any
 * dynamic / data-driven overrides.
 */
public final class StackedDimensions {
    private StackedDimensions() {
    }

    /** Resource location of the stacked-overworld sub-dim. */
    public static final ResourceLocation OVERWORLD_ID =
            ResourceLocation.fromNamespaceAndPath("cubicchunks", "overworld_stacked");

    /** Resource location of the stacked-nether sub-dim. */
    public static final ResourceLocation NETHER_ID =
            ResourceLocation.fromNamespaceAndPath("cubicchunks", "nether_stacked");

    /** Resource location of the stacked-end sub-dim. */
    public static final ResourceLocation END_ID =
            ResourceLocation.fromNamespaceAndPath("cubicchunks", "end_stacked");

    /** Resource location of bedrock — used as the top/bottom fill for the stacked sub-dims. */
    public static final ResourceLocation BEDROCK_ID =
            ResourceLocation.withDefaultNamespace("bedrock");

    /** Resource location of stone — used as the overworld fill block for extension cubes. */
    public static final ResourceLocation STONE_ID =
            ResourceLocation.withDefaultNamespace("stone");

    /** Resource location of netherrack — the per-band fill block of the Nether. */
    public static final ResourceLocation NETHERRACK_ID =
            ResourceLocation.withDefaultNamespace("netherrack");

    /** Resource location of end_stone — the per-band fill block of The End. */
    public static final ResourceLocation END_STONE_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "end_stone");

    /**
     * Default stacked-overworld sub-dim: matches the vanilla overworld build window
     * with allocations for cubic fill above and below.
     */
    public static final StackedDimension OVERWORLD = new StackedDimension(
            OVERWORLD_ID, "Overworld",
            -64, 320,
            StackedDimensionPalette.OVERWORLD,
            STONE_ID,
            true /* bedrockTop */,
            true /* bedrockBottom */
    );

    /**
     * Default stacked-nether sub-dim. Lives strictly below the overworld's bedrock
     * floor (Y=−64): the band's upper bound is -65 so there is a 1-block buffer
     * between the overworld floor and the nether ceiling, satisfying the registry
     * overlap check. Range height (-192..-65) = 128 blocks, matching vanilla's
     * Nether height so per-band structures (lava lakes, glowstone blobs) have the
     * same vertical room as the standalone dimension.
     */
    public static final StackedDimension NETHER = new StackedDimension(
            NETHER_ID, "Nether (stacked)",
            -192, -65,
            StackedDimensionPalette.NETHER,
            NETHERRACK_ID,
            true /* bedrockTop */,
            true /* bedrockBottom */
    );

    /**
     * Default stacked-end sub-dim. Lives ~12,000 blocks above the overworld's Y top
     * (Y=320) so the gap between Overworld and End (12,000 blocks of air) is the
     * "vertical buffer" the user requested. Range height 512 blocks matches the
     * standalone End dimension's vertical room.
     */
    public static final StackedDimension END = new StackedDimension(
            END_ID, "The End (stacked)",
            12320, 12832,
            StackedDimensionPalette.END,
            END_STONE_ID,
            true /* bedrockTop */,
            true /* bedrockBottom */
    );

    /**
     * Returns the cube-Y range covered by the default overworld sub-dim, expressed as
     * {@code [min, max]} inclusive. Convenience override of {@link StackedDimension#getMinCubeY()}
     * so callers don't have to construct the full record.
     */
    public static int overworldMinCubeY() {
        return Coords.blockToCube(OVERWORLD.minBlockY());
    }

    public static int overworldMaxCubeY() {
        return Coords.blockToCube(OVERWORLD.maxBlockY());
    }

    public static int netherMinCubeY() {
        return Coords.blockToCube(NETHER.minBlockY());
    }

    public static int netherMaxCubeY() {
        return Coords.blockToCube(NETHER.maxBlockY());
    }

    public static int endMinCubeY() {
        return Coords.blockToCube(END.minBlockY());
    }

    public static int endMaxCubeY() {
        return Coords.blockToCube(END.maxBlockY());
    }
}
