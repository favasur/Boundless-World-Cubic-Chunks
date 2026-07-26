package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType
public interface ICubicWorldType {
    @Nullable
    ICubeGenerator createCubeGenerator(Level world);

    IntRange calculateGenerationHeightRange(ServerLevel world);

    boolean hasCubicGeneratorForWorld(Level world);
}
