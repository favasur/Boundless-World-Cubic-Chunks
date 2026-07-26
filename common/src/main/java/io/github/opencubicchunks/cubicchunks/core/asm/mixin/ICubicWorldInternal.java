package io.github.opencubicchunks.cubicchunks.core.asm.mixin;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal
public interface ICubicWorldInternal extends ICubicWorld {
    void initCubicWorld();

    void initCubicWorldClient();

    void tickCubicWorld();

    void fakeWorldHeight(int height);

    LightingManager getLightingManager();

    void setCubeCache(io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider provider);
}
