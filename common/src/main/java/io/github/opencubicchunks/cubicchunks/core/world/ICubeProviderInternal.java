package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal
public interface ICubeProviderInternal extends ICubeProvider {
    @Nullable
    Cube getLoadedCube(int x, int y, int z);

    @Nullable
    Cube getLoadedCube(CubePos pos);

    Cube getCube(int x, int y, int z);

    Cube getCube(CubePos pos);

    interface Server extends ICubeProviderInternal {
        ICubeIO getCubeIO();
    }
}
