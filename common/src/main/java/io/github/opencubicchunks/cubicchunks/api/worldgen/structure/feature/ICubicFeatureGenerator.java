package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature.ICubicFeatureGenerator
public interface ICubicFeatureGenerator {
    @Nullable
    Object decorate(Level level, CubePos pos);
}
