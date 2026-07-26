package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubicPopulatorList
public class CubicPopulatorList {
    private final List<ICubicPopulator> populators = new ArrayList<>();

    public List<ICubicPopulator> getPopulators() {
        return this.populators;
    }

    public void register(ICubicPopulator populator) {
        if (populator != null) {
            this.populators.add(populator);
        }
    }

    public void generateAll(Level level, Random random, CubePos pos, Holder<Biome> biome,
                             io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer primer) {
        for (ICubicPopulator p : this.populators) {
            try {
                p.generate(level, random, pos, biome, primer);
            } catch (Throwable t) {
                CubicChunks.LOGGER.error("CubicPopulator {} failed at {}", p.getClass().getSimpleName(), pos, t);
            }
        }
    }
}
