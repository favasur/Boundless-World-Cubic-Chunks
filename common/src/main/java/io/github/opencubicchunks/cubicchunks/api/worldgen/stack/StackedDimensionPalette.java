package io.github.opencubicchunks.cubicchunks.api.worldgen.stack;

import java.util.Optional;

/**
 * Per-sub-dim ambient settings: sky color, fog color, water color, ambient-light level,
 * sky-light availability, weather and bed-safety flag. Listener mixins (sky / fog /
 * ClientLevel ambient) read from this record to swap the visual presentation when the
 * camera traverses between stacked sub-dim bands.
 *

 * @param skyColorRgb        packed RGB (0xRRGGBB) of the sky overhead in this sub-dim
 * @param fogColorRgb        packed RGB of fog
 * @param waterColorRgb      packed RGB of water tint
 * @param grassColorRgb      optional packed RGB of grass tint (Integer.MIN_VALUE = leave to biome)
 * @param ambientLight       ambient sky-light floor (0..15) at this sub-dim
 * @param hasSkyLight        true if blocks in this sub-dim receive sky light
 * @param hasCeiling         true if the sub-dim has a ceiling (Nether-style, blocks sky-light)
 * @param ultrawarm          true if water evaporates and beds explode in this sub-dim
 * @param natural            true if the sub-dim should be considered "natural" for mobs/pathfinding
 * @param piglinSafe         true if zombified piglins stay neutral in this sub-dim
 * @param bedWorks           true if players can set spawn / sleep through a night in this sub-dim
 * @param respawnAnchorWorks true if respawn anchors are valid in this sub-dim
 * @param hasRaids           true if patrols and raids can trigger here
 * @param skylightDuringDay  whether sky light waxes/wanes by day cycle (false = constant ambient)
 */
public record StackedDimensionPalette(
        int skyColorRgb,
        int fogColorRgb,
        int waterColorRgb,
        int grassColorRgb,
        int ambientLight,
        boolean hasSkyLight,
        boolean hasCeiling,
        boolean ultrawarm,
        boolean natural,
        boolean piglinSafe,
        boolean bedWorks,
        boolean respawnAnchorWorks,
        boolean hasRaids,
        boolean skylightDuringDay
) {
    /** Vanilla overworld palette (approximate defaults). */
    public static final StackedDimensionPalette OVERWORLD = new StackedDimensionPalette(
            0x78A7FF, 0xC0D8FF, 0x3F76E4, Integer.MIN_VALUE,
            0, true, false, false, true, false, true, false, true, true
    );

    /** Vanilla Nether palette — warm, foggy, no ceiling-light. */
    public static final StackedDimensionPalette NETHER = new StackedDimensionPalette(
            0xB12522, 0x2D0808, 0xA51E22, 0xB1B154,
            4, false, true, true, false, true, false, true, false, false
    );

    /** The End palette — dark, low ambient. */
    public static final StackedDimensionPalette END = new StackedDimensionPalette(
            0x000000, 0x0E1018, 0x6253B5, Integer.MIN_VALUE,
            4, false, false, false, false, false, false, false, false, true
    );

    public Optional<Integer> grassColorOverride() {
        return grassColorRgb == Integer.MIN_VALUE ? Optional.empty() : Optional.of(grassColorRgb);
    }
}
