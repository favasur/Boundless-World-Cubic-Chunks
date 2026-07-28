package io.github.opencubicchunks.cubicchunks.core.world.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Random;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.CubeWorldEntitySpawner
// 1.21: drives spawns across cubes. Each cube shares a capped count per MobCategory with
// neighbouring cubes via a per-column shared budget, and an entity is only placed after
// verifying its destination cube is loaded.
public class CubicPopulationRunner implements IWorldEntitySpawner {

    private final ServerLevel level;
    private final int ticksBetweenSpawns;

    public CubicPopulationRunner(ServerLevel level) {
        this.level = level;
        this.ticksBetweenSpawns = 200;
    }

    @Override
    public int countEntitiesInCube(io.github.opencubicchunks.cubicchunks.api.world.ICube cube, MobCategory category) {
        int count = 0;
        for (var entity : cube.getEntitySet()) {
            if (entity.getType().getCategory() == category && entity.isAlive()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<MobSpawnSettings.SpawnerData> getSpawnableForCube(
            io.github.opencubicchunks.cubicchunks.api.world.ICube cube, MobCategory category, BlockPos pos) {
        var biome = level.getBiome(pos).value();
        if (biome.getMobSettings() == null) return List.of();
        java.util.List<MobSpawnSettings.SpawnerData> collected = new java.util.ArrayList<>();
        try {
            biome.getMobSettings().getMobs(category).unwrap().forEach(collected::add);
        } catch (Throwable t) {
            return List.of();
        }
        return collected;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        var provider = (CubeProviderServer) ((ICubicWorldInternal) level).getCubeCache();
        for (io.github.opencubicchunks.cubicchunks.core.world.cube.Cube columnCube : provider.getLoadedCubes()) {
            io.github.opencubicchunks.cubicchunks.api.world.IColumn col = columnCube.getColumn();
            if (!(col instanceof LevelChunk chunk)) continue;
            int columnX = chunk.getPos().x;
            int columnZ = chunk.getPos().z;
            int sectionCount = chunk.getSections().length;
            int minSectionY = chunk.getMinSection();

            // Per-column shared entity count, grouped by MobCategory so we know how
            // much of each budget we're allowed to use across this column's cubes.
            int[] categoryUsed = new int[MobCategory.values().length];
            int[] categoryMax = new int[MobCategory.values().length];
            for (MobCategory cat : MobCategory.values()) {
                categoryMax[cat.ordinal()] = cat.getMaxInstancesPerChunk() * sectionCount;
            }

            for (int sy = 0; sy < sectionCount; sy++) {
                int cubeY = minSectionY + sy;
                var section = chunk.getSections()[sy];
                if (section == null || section.hasOnlyAir()) continue;
                var cubePos = CubePos.of(columnX, cubeY, columnZ);
                io.github.opencubicchunks.cubicchunks.core.world.cube.Cube cube = provider.getLoadedCube(cubePos);
                if (cube == null || !cube.isCubeLoaded()) continue;

                RandomSource rand = level.getRandom();
                for (MobCategory cat : MobCategory.values()) {
                    int catIndex = cat.ordinal();
                    int currentThisCube = countEntitiesInCube(cube, cat);
                    if (categoryUsed[catIndex] + currentThisCube >= categoryMax[catIndex]) continue;
                    int remainingBudget = categoryMax[catIndex] - (categoryUsed[catIndex] + currentThisCube);
                    int spawns = (int) Math.ceil(remainingBudget * 0.05);
                    for (int i = 0; i < spawns; i++) {
                        BlockPos pos = pickSpawnPos(cube, rand, chunk);
                        if (pos == null) continue;
                        var entries = getSpawnableForCube(cube, cat, pos);
                        if (entries == null || entries.isEmpty()) continue;
                        var data = entries.get(rand.nextInt(entries.size()));
                        if (data == null || data.type == null) continue;
                        try {
                            var entity = data.type.create(level);
                            if (!(entity instanceof LivingEntity living)) continue;
                            living.moveTo(
                                    pos.getX() + rand.nextDouble(),
                                    pos.getY(),
                                    pos.getZ() + rand.nextDouble(),
                                    rand.nextFloat() * 360F - 180F,
                                    0F);
                            level.addFreshEntityWithPassengers(living);
                            categoryUsed[catIndex]++;
                            if (categoryUsed[catIndex] >= categoryMax[catIndex]) break;
                        } catch (Throwable t) {
                            CubicChunks.LOGGER.warn("Failed to spawn {} in cube {}", cat, cubePos, t);
                        }
                    }
                }
            }
        }
    }

    private BlockPos pickSpawnPos(io.github.opencubicchunks.cubicchunks.api.world.ICube cube,
                                   RandomSource rand, LevelChunk chunk) {
        var section = ((io.github.opencubicchunks.cubicchunks.core.world.cube.Cube) cube).getStorage();
        if (section == null) return null;
        int localX = rand.nextInt(16);
        int localZ = rand.nextInt(16);
        int localY = rand.nextInt(16);
        int blockX = (cube.getCoords().getX() << 4) + localX;
        int blockY = (cube.getCoords().getY() << 4) + localY;
        int blockZ = (cube.getCoords().getZ() << 4) + localZ;
        // Ensure the destination chunk matches the cube's column. If not, the spawn is
        // likely outside the loaded set so we skip.
        if ((blockX >> 4) != chunk.getPos().x || (blockZ >> 4) != chunk.getPos().z) return null;
        return new BlockPos(blockX, blockY, blockZ);
    }
}
