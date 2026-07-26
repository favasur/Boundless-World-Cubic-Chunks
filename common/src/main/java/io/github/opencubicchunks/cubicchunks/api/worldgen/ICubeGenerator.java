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

    default Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force) {
        return Optional.of(generateCube(cubeX, cubeY, cubeZ, primer));
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
