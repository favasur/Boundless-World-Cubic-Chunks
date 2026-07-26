package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner
public interface IWorldEntitySpawner {
    int countEntitiesInCube(ICube cube, MobCategory category);

    List<MobSpawnSettings.SpawnerData> getSpawnableForCube(ICube cube, MobCategory category, BlockPos pos);

    void serverTick(ServerLevel level);
}
