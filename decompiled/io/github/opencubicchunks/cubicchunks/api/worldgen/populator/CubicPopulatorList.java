package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CubicPopulatorList implements ICubicPopulator {
   private List<ICubicPopulator> list = new ArrayList<>();

   public CubicPopulatorList(List<ICubicPopulator> populators) {
      this();
      this.list.addAll(populators);
   }

   public CubicPopulatorList() {
   }

   public void add(ICubicPopulator populator) {
      this.list.add(populator);
   }

   public void makeImmutable() {
      this.list = Collections.unmodifiableList(this.list);
   }

   @Override
   public void generate(World world, Random random, CubePos pos, Biome biome) {
      this.list.forEach(p -> p.generate(world, random, pos, biome));
   }
}
