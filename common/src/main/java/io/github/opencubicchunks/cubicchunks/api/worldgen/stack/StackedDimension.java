package io.github.opencubicchunks.cubicchunks.api.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import net.minecraft.resources.ResourceLocation;

/**
 * A vertically-stacked sub-dimension that lives inside the overworld's save file.
 *
 * <p>Each {@code StackedDimension} owns a contiguous Y range and its own ambient palette
 * (sky / fog / water / weather / ambient-light settings). The overworld cube provider
 * dispatches generation and gameplay-event behavior to the matching sub-dim based on
 * the cube's block-Y coordinate. Vanilla Nether / End worlds are absorbed into this
 * scheme by mapping their vanilla Y ranges onto our stacked Y bands.</p>
 *
 * @param id           unique resource location, used in save data and registry lookups
 * @param displayName  human-readable name shown in chat / command feedback
 * @param minBlockY    inclusive lowest block Y for this sub-dim
 * @param maxBlockY    inclusive highest block Y for this sub-dim
 * @param palette      sky / fog / ambient-light / weather details
 * @param fillBlockId  resource location of the block that fills "above-the-ceiling" cubes
 *                     in this sub-dim (Nether uses netherrack, End uses end_stone,
 *                     Overworld uses stone).
 * @param bedrockTop   true if this sub-dim should put a bedrock ceiling at the top of its range
 * @param bedrockBottom true if this sub-dim should put a bedrock floor at the bottom of its range
 */
public record StackedDimension(
        ResourceLocation id,
        String displayName,
        int minBlockY,
        int maxBlockY,
        StackedDimensionPalette palette,
        ResourceLocation fillBlockId,
        boolean bedrockTop,
        boolean bedrockBottom
) {
    /**
     * Lowest cube Y that belongs to this sub-dim (inclusive). Cube Y is measured in
     * 16-block steps and follows floor-division {@code blockY >> 4} semantics.
     */
    public int getMinCubeY() {
        return Coords.blockToCube(this.minBlockY);
    }

    /**
     * Highest cube Y that belongs to this sub-dim (inclusive).
     */
    public int getMaxCubeY() {
        return Coords.blockToCube(this.maxBlockY);
    }

    /**
     * True if the given cube Y sits inside this sub-dim's block range.
     */
    public boolean containsCubeY(int cubeY) {
        return cubeY >= this.getMinCubeY() && cubeY <= this.getMaxCubeY();
    }

    /**
     * True if the given block Y sits inside this sub-dim.
     */
    public boolean containsBlockY(int blockY) {
        return blockY >= this.minBlockY && blockY <= this.maxBlockY;
    }

    /**
     * Maps the given block Y to a 0..15 local cube-local Y at the same cubeY. Useful
     * for per-band noise generation that expects a 0..15 range.
     */
    public int localY(int cubeY, int blockY) {
        return Coords.blockToLocal(blockY);
    }
}
