package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.lighting.ILightingManager
public interface ILightingManager {
    void doOnBlockSetLightUpdates(LevelChunk column, int localX, int y1, int y2, int localZ);

    void onTick();

    void markCubeBlockColumnForUpdate(ICube cube, int localX, int localZ);

    boolean checkLightFor(LightLayer lightType, BlockPos pos);
}
