package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;

/**
 * Internal cubic world interface — moved out of the mixin package
 * {@code io.github.opencubicchunks.cubicchunks.core.asm.mixin} because
 * Mixin blocks non-@Mixin classes from referencing interfaces in packages
 * owned by mixin configs ({@code IllegalClassLoadError}).
 */
public interface ICubicWorldInternal extends ICubicWorld {
    void initCubicWorld();

    void initCubicWorldClient();

    void tickCubicWorld();

    void fakeWorldHeight(int height);

    LightingManager getLightingManager();

    void setCubeCache(io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider provider);
}
