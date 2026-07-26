package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer
public interface ICubicWorldServer extends ICubicWorld {
    ICubeProviderServer getCubeCache();

    ICubeGenerator getCubeGenerator();

    void unloadOldCubes();

    void forceChunk(Object ticket, CubePos pos);

    void reorderChunk(Object ticket, CubePos pos);

    void unforceChunk(Object ticket, CubePos pos);
}
