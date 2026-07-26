package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Initial-worldgen mob spawn pass. 1.21 port: {@code MobSpawnSettings.getMobs}
 * returns a {@code WeightedRandomList<SpawnerData>} (1.21 lives under
 * {@code net.minecraft.util.random}), so the local variable type stays
 * {@code WeightedRandomList<SpawnerData>}; only the package import changes.
 */
public class WorldGenEntitySpawner {
    public WorldGenEntitySpawner() {
    }

    public static void initialWorldGenSpawn(ServerLevel world, Biome biome,
                                             int blockX, int blockY, int blockZ,
                                             int sizeX, int sizeY, int sizeZ,
                                             RandomSource random) {
        MobSpawnSettings settings = biome.getMobSettings();
        if (settings == null) return;
        WeightedRandomList<MobSpawnSettings.SpawnerData> spawnList = settings.getMobs(MobCategory.CREATURE);
        if (spawnList.isEmpty()) return;

        float chance = settings.getCreatureProbability();
        while (random.nextFloat() < chance) {
            MobSpawnSettings.SpawnerData currEntry = spawnList.getRandom(random).orElse(null);
            if (currEntry == null || currEntry.type == null) continue;
            int groupCount = Mth.nextInt(random, currEntry.minCount, currEntry.maxCount);
            for (int i = 0; i < groupCount; i++) {
                for (int j = 0; j < 4; j++) {
                    int randX = blockX + random.nextInt(sizeX);
                    int randZ = blockZ + random.nextInt(sizeZ);
                    BlockPos pos = new BlockPos(randX, blockY + sizeY + 8, randZ);
                    if (pos.getY() < world.getMinBuildHeight() || pos.getY() >= world.getMaxBuildHeight()) continue;
                    try {
                        Entity entity = currEntry.type.create(world);
                        if (!(entity instanceof LivingEntity)) continue;
                        entity.moveTo(randX + 0.5, pos.getY(), randZ + 0.5,
                                random.nextFloat() * 360F, 0F);
                        world.addFreshEntityWithPassengers(entity);
                        break;
                    } catch (Throwable t) {
                        // Some mob constructors may throw for invalid dimensions; skip.
                    }
                }
            }
        }
    }
}
