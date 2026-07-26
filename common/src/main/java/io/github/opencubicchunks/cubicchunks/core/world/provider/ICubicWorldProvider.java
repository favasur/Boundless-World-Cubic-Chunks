package io.github.opencubicchunks.cubicchunks.core.world.provider;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider
public interface ICubicWorldProvider {
    @Nullable
    ICubeGenerator createCubeGenerator(Level world);

    boolean isCubic(Level world);
}
