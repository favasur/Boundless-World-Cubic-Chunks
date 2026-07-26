package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.worldgen.populator.CubicPopulationRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.CubeWorldEntitySpawner
public class CubeWorldEntitySpawner implements IWorldEntitySpawner {

    private final CubicPopulationRunner runner;

    public CubeWorldEntitySpawner(ServerLevel level) {
        this.runner = new CubicPopulationRunner(level);
    }

    @Override
    public int countEntitiesInCube(ICube cube, MobCategory category) {
        return this.runner.countEntitiesInCube(cube, category);
    }

    @Override
    public List<MobSpawnSettings.SpawnerData> getSpawnableForCube(ICube cube, MobCategory category, BlockPos pos) {
        return this.runner.getSpawnableForCube(cube, category, pos);
    }

    @Override
    public void serverTick(ServerLevel level) {
        this.runner.serverTick(level);
    }
}
