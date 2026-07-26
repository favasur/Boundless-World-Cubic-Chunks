package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;

import java.util.Random;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator
public interface ICubicPopulator {
    void generate(Level level, Random random, CubePos pos, Holder<net.minecraft.world.level.biome.Biome> biome, CubePrimer primer);
}
