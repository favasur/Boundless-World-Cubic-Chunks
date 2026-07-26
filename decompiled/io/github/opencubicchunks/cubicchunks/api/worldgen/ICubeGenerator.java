package io.github.opencubicchunks.cubicchunks.api.worldgen;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeGenerator {
   Box RECOMMENDED_FULL_POPULATOR_REQUIREMENT = new Box(-1, -1, -1, 0, 0, 0);
   Box RECOMMENDED_GENERATE_POPULATOR_REQUIREMENT = new Box(1, 1, 1, 0, 0, 0);
   Box NO_REQUIREMENT = new Box(0, 0, 0, 0, 0, 0);

   @Deprecated
   CubePrimer generateCube(int var1, int var2, int var3);

   default CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
      return this.generateCube(cubeX, cubeY, cubeZ);
   }

   void generateColumn(Chunk var1);

   void populate(ICube var1);

   default Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean forceGenerate) {
      return Optional.of(this.generateCube(cubeX, cubeY, cubeZ, primer));
   }

   default Optional<Chunk> tryGenerateColumn(World world, int columnX, int columnZ, ChunkPrimer primer, boolean forceGenerate) {
      Chunk column = new Chunk(world, columnX, columnZ);
      this.generateColumn(column);
      return Optional.of(column);
   }

   default boolean supportsConcurrentCubeGeneration() {
      return false;
   }

   default boolean supportsConcurrentColumnGeneration() {
      return false;
   }

   default ICubeGenerator.GeneratorReadyState pollAsyncCubeGenerator(int cubeX, int cubeY, int cubeZ) {
      return ICubeGenerator.GeneratorReadyState.READY;
   }

   default ICubeGenerator.GeneratorReadyState pollAsyncColumnGenerator(int chunkX, int chunkZ) {
      return ICubeGenerator.GeneratorReadyState.READY;
   }

   default ICubeGenerator.GeneratorReadyState pollAsyncCubePopulator(int cubeX, int cubeY, int cubeZ) {
      return ICubeGenerator.GeneratorReadyState.READY;
   }

   Box getFullPopulationRequirements(ICube var1);

   Box getPopulationPregenerationRequirements(ICube var1);

   void recreateStructures(ICube var1);

   void recreateStructures(Chunk var1);

   List<SpawnListEntry> getPossibleCreatures(EnumCreatureType var1, BlockPos var2);

   @Nullable
   BlockPos getClosestStructure(String var1, BlockPos var2, boolean var3);

   public static enum GeneratorReadyState {
      READY,
      WAITING,
      FAIL;

      private GeneratorReadyState() {
      }
   }
}
