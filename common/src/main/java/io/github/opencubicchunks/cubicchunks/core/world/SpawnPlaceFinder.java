package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.SpawnPlaceFinder
// 1.21: simplified, walks Heightmap.WORLD_SURFACE at the spawn chunk pos for the
// tallest cubeY in view.
public final class SpawnPlaceFinder {

    private SpawnPlaceFinder() {
    }

    public static BlockPos find(ServerLevel level, ChunkPos chunkPos) {
        int searchRadius = level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS);
        BlockPos levelSpawn = level.getSharedSpawnPos();
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int worldX = (chunkPos.x << 4) + dx * 16 + 8;
                int worldZ = (chunkPos.z << 4) + dz * 16 + 8;
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(worldX, 0, worldZ));
                BlockState underState = level.getBlockState(surface.below());
                if (underState.getBlock() == Blocks.GRASS_BLOCK || underState.getBlock() == Blocks.SAND || underState.getBlock() == Blocks.DIRT) {
                    return surface;
                }
            }
        }
        return levelSpawn;
    }

    public static CubePos findSpawnCube(ServerLevel level) {
        BlockPos spawn = find(level, new ChunkPos(level.getSharedSpawnPos()));
        return CubePos.of(spawn.getX() >> 4, spawn.getY() >> 4, spawn.getZ() >> 4);
    }
}
