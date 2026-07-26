package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

import java.util.Random;

/**
 * Pluggable per-band cube generation strategy. Each stacked sub-dim registered in
 * {@link StackedDimensionRegistry} gets a strategy: for the default Nether band the
 * strategy wraps {@code NetherChunkGenerator} and reads its biome source; for the End
 * band it wraps {@code EndChunkGenerator}.
 *
 * <p>Strategies own two responsibilities:</p>
 * <ol>
 *     <li><b>Generation</b>: fill a {@link CubePrimer} with biome tags and
 *         per-band-shaped blocks for the cube at {@code (cubeX, cubeY, cubeZ)}.</li>
 *     <li><b>Population</b>: fire per-band decoration (mob spawns, structure hints)
 *         as part of the cube's first-load flow.</li>
 * </ol>
 */
public interface StackedBandStrategy {
    /**
     * Generate the cube at the given position, writing block states and biomes into
     * {@code primer}. The implementation should fill ALL blocks; partial fills are
     * supported (pre-existing primer content is overwritten).
     */
    CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer);

    /**
     * Optional population pass for cubes in this band. Called from the cube provider
     * after the cube is loaded, before world ticking.
     */
    default void populate(ICube cube, Random rand) {
    }

    /**
     * Inverse accessor: which stacked dim does this strategy own? Used for diagnostic
     * messages on dispatch failures.
     */
    StackedDimension getDimension();

    /**
     * Convenience accessor for level access when the strategy needs to query other
     * generators (biome fallback, registry, etc.).
     */
    ServerLevel getLevel();

    /**
     * Returns the per-band {@link ChunkGenerator} if the strategy owns one (Nether
     * uses {@code minecraft:nether}, End uses {@code minecraft:end}). Returns null
     * for generic fill strategies. Feature placement looks up this generator via
     * {@link StackedCubeGenerator#recreateStructures} and the per-band populate
     * pass.
     */
    default ChunkGenerator getChunkGenerator() {
        return null;
    }

    /**
     * Returns the per-band {@link ChunkGeneratorStructureState} if the strategy
     * pre-built one from its chunk generator and biome source. Used by
     * {@code BandedFeaturePlacer.tryCreateStructuresForColumn} so vanilla structure
     * placement biome lookups stay inside the band's noise domain. Returns null
     * for strategies that don't own a real {@link #getChunkGenerator()}.
     */
    default ChunkGeneratorStructureState getChunkGeneratorState() {
        return null;
    }
}
