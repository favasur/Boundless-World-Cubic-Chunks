package io.github.opencubicchunks.cubicchunks.api.worldgen.structure;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator
public interface ICubicStructureGenerator {
    @Nullable
    Object generate(Level level, CubePos pos);
}
