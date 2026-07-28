package io.github.opencubicchunks.cubicchunks.api.worldgen;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator
// 1.21: added Optional<CubePrimer> tryGenerateCube that returns missing-cell semantics, plus
// the registerBlocks hook used by the original (was a no-op for vanilla-wrapped generators).
public interface ICubeGenerator {

    Box NO_REQUIREMENT = new Box(0, 0, 0, 0, 0, 0);

    CubePrimer generateCube(int cubeX, int cubeY, int cubeZ);

    CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer);

    /**
     * 1.21 port: variant that accepts a column already loaded by the cube provider,
     * so the generator can read directly from {@code sections[]} at the correct
     * chunk status instead of doing its own {@code getChunk(false)} round-trip.
     * The default falls back to {@link #generateCube(int, int, int, CubePrimer)}.
     */
    default CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, @org.jetbrains.annotations.Nullable ChunkAccess preloadedColumn) {
        return generateCube(cubeX, cubeY, cubeZ, primer);
    }

    default Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force) {
        return Optional.of(generateCube(cubeX, cubeY, cubeZ, primer));
    }

    /**
     * 1.21 port: tryGenerateCube variant that accepts a column already loaded by
     * the cube provider, so the generator can read directly from {@code sections[]}
     * at the correct chunk status instead of doing its own {@code getChunk(false)}
     * round-trip. The default delegates to {@link #tryGenerateCube(int, int, int,
     * CubePrimer, boolean)}.
     */
    default Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force,
                                                 @org.jetbrains.annotations.Nullable ChunkAccess preloadedColumn) {
        return tryGenerateCube(cubeX, cubeY, cubeZ, primer, force);
    }

    void generateColumn(ChunkAccess column);

    void populate(ICube cube);

    Box getFullPopulationRequirements(ICube cube);

    Box getPopulationPregenerationRequirements(ICube cube);

    void recreateStructures(ICube cube);

    void recreateStructures(ChunkAccess column);

    default void registerBlocks() {
    }

    List<MobSpawnSettings.SpawnerData> getPossibleCreatures(MobCategory category, BlockPos pos);

    @Nullable
    default BlockPos getClosestStructure(String name, BlockPos pos, boolean allowUnexplored) {
        return null;
    }
}
