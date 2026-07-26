package io.github.opencubicchunks.cubicchunks.api.world;

import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer
public interface ICubeProviderServer extends ICubeProvider {
    @Nullable
    ChunkAccess getColumn(int x, int z, Requirement requirement);

    @Nullable
    ICube getCube(int x, int y, int z, Requirement requirement);

    @Nullable
    ICube getCubeNow(int x, int y, int z, Requirement requirement);

    boolean isCubeGenerated(int x, int y, int z);

    enum Requirement {
        GET_CACHED,
        LOAD,
        GENERATE,
        POPULATE,
        LIGHT
    }
}
