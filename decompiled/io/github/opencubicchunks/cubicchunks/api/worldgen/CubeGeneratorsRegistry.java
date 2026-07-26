package io.github.opencubicchunks.cubicchunks.api.worldgen;

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.ICubicPopulator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CubeGeneratorsRegistry {
   private static final List<ICubicPopulator> customPopulatorsForFlatCubicGenerator = new ArrayList<>();
   private static final List<BiConsumer<? super World, ? super LoadingData<CubePos>>> cubeLoadingCallbacks = new ArrayList<>(2);
   private static final List<BiConsumer<? super World, ? super LoadingData<ChunkPos>>> columnLoadingCallbacks = new ArrayList<>(2);
   private static final Collection<BiConsumer<? super World, ? super LoadingData<CubePos>>> cubeLoadingCallbacksView = Collections.unmodifiableCollection(
      cubeLoadingCallbacks
   );
   private static final Collection<BiConsumer<? super World, ? super LoadingData<ChunkPos>>> columnLoadingCallbacksView = Collections.unmodifiableCollection(
      columnLoadingCallbacks
   );
   private static final TreeSet<CubeGeneratorsRegistry.GeneratorWrapper> sortedGeneratorList = new TreeSet<>();

   public CubeGeneratorsRegistry() {
   }

   public static void register(ICubicPopulator populator, int weight) {
      Preconditions.checkNotNull(populator);
      sortedGeneratorList.add(new CubeGeneratorsRegistry.GeneratorWrapper(populator, weight));
   }

   public static void generateWorld(World world, Random random, CubePos pos, Biome biome) {
      for (CubeGeneratorsRegistry.GeneratorWrapper wrapper : sortedGeneratorList) {
         wrapper.populator.generate(world, random, pos, biome);
      }
   }

   public static void registerForCompatibilityGenerator(ICubicPopulator populator) {
      if (!customPopulatorsForFlatCubicGenerator.contains(populator)) {
         customPopulatorsForFlatCubicGenerator.add(populator);
      }
   }

   public static void populateVanillaCubic(World world, Random rand, ICube cube) {
      for (ICubicPopulator populator : customPopulatorsForFlatCubicGenerator) {
         populator.generate(world, rand, cube.getCoords(), cube.getBiome(cube.getCoords().getCenterBlockPos()));
      }
   }

   public static void registerCubeAsyncLoadingCallback(BiConsumer<? super World, ? super LoadingData<CubePos>> cubeCallback) {
      cubeLoadingCallbacks.add(cubeCallback);
   }

   public static void registerColumnAsyncLoadingCallback(BiConsumer<? super World, ? super LoadingData<ChunkPos>> columnCallback) {
      columnLoadingCallbacks.add(columnCallback);
   }

   public static Collection<BiConsumer<? super World, ? super LoadingData<CubePos>>> getCubeAsyncLoadingCallbacks() {
      return cubeLoadingCallbacksView;
   }

   public static Collection<BiConsumer<? super World, ? super LoadingData<ChunkPos>>> getColumnAsyncLoadingCallbacks() {
      return columnLoadingCallbacksView;
   }

   private static class GeneratorWrapper implements Comparable<CubeGeneratorsRegistry.GeneratorWrapper> {
      private final ICubicPopulator populator;
      private final int weight;

      public GeneratorWrapper(ICubicPopulator populator, int weight) {
         this.populator = populator;
         this.weight = weight;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (!(o instanceof CubeGeneratorsRegistry.GeneratorWrapper)) {
            return false;
         } else {
            CubeGeneratorsRegistry.GeneratorWrapper that = (CubeGeneratorsRegistry.GeneratorWrapper)o;
            return this.weight != that.weight ? false : this.populator.equals(that.populator);
         }
      }

      @Override
      public int hashCode() {
         int result = this.populator.hashCode();
         return 31 * result + this.weight;
      }

      public int compareTo(CubeGeneratorsRegistry.GeneratorWrapper o) {
         return Integer.compare(this.weight, o.weight);
      }
   }
}
