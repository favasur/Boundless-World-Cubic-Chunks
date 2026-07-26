package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public interface ICubicPopulator {
   void generate(World var1, Random var2, CubePos var3, Biome var4);
}
