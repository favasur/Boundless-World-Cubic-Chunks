package io.github.opencubicchunks.cubicchunks.core.worldgen.generator.vanilla;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IGameRegistry;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.worldgen.WorldgenHangWatchdog;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VanillaCompatibilityGenerator implements ICubeGenerator {
   private boolean isInit = false;
   private int worldHeightCubes;
   @Nonnull
   private final IChunkGenerator vanilla;
   @Nonnull
   private final World world;
   private Chunk lastChunk;
   private boolean optimizationHack;
   private Biome[] biomes;
   @Nonnull
   private IBlockState extensionBlockBottom = Blocks.field_150348_b.func_176223_P();
   @Nonnull
   private IBlockState extensionBlockTop = Blocks.field_150350_a.func_176223_P();
   private boolean hasTopBedrock = false;
   private boolean hasBottomBedrock = true;

   public VanillaCompatibilityGenerator(IChunkGenerator vanilla, World world) {
      this.vanilla = vanilla;
      this.world = world;
   }

   private void tryInit(IChunkGenerator vanilla, World world) {
      if (!this.isInit) {
         this.isInit = true;
         this.lastChunk = vanilla.func_185932_a(0, 0);
         int worldHeightBlocks = ((ICubicWorld)world).getMaxGenerationHeight();
         this.worldHeightCubes = worldHeightBlocks / 16;
         Map<IBlockState, Integer> blockHistogramBottom = new HashMap<>();
         Map<IBlockState, Integer> blockHistogramTop = new HashMap<>();
         ExtendedBlockStorage bottomEBS = this.lastChunk.func_76587_i()[0];

         for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 3; y++) {
                  IBlockState blockState = bottomEBS == null ? Blocks.field_150350_a.func_176223_P() : bottomEBS.func_177485_a(x, y, z);
                  int count = blockHistogramBottom.getOrDefault(blockState, 0);
                  blockHistogramBottom.put(blockState, count + 1);
               }

               for (int y = worldHeightBlocks - 1; y > worldHeightBlocks - 4; y--) {
                  int localY = Coords.blockToLocal(y);
                  ExtendedBlockStorage ebs = this.lastChunk.func_76587_i()[Coords.blockToCube(y)];
                  IBlockState blockState = ebs == null ? Blocks.field_150350_a.func_176223_P() : ebs.func_177485_a(x, localY, z);
                  int count = blockHistogramTop.getOrDefault(blockState, 0);
                  blockHistogramTop.put(blockState, count + 1);
               }
            }
         }

         CubicChunks.LOGGER.debug("Block histograms: \nTop: " + blockHistogramTop + "\nBottom: " + blockHistogramBottom);
         int topcount = 0;

         for (Entry<IBlockState, Integer> entry : blockHistogramBottom.entrySet()) {
            if (entry.getValue() > topcount && entry.getKey().func_177230_c() != Blocks.field_150357_h) {
               this.extensionBlockBottom = entry.getKey();
               topcount = entry.getValue();
            }
         }

         this.hasBottomBedrock = blockHistogramBottom.getOrDefault(Blocks.field_150357_h.func_176223_P(), 0) > 0;
         CubicChunks.LOGGER
            .info(
               "Detected filler block " + this.extensionBlockBottom.func_177230_c().func_149739_a() + " from layers [0, 2], bedrock=" + this.hasBottomBedrock
            );
         topcount = 0;

         for (Entry<IBlockState, Integer> entryx : blockHistogramTop.entrySet()) {
            if (entryx.getValue() > topcount && entryx.getKey().func_177230_c() != Blocks.field_150357_h) {
               this.extensionBlockTop = entryx.getKey();
               topcount = entryx.getValue();
            }
         }

         this.hasTopBedrock = blockHistogramTop.getOrDefault(Blocks.field_150357_h.func_176223_P(), 0) > 0;
         CubicChunks.LOGGER
            .info(
               "Detected filler block "
                  + this.extensionBlockTop.func_177230_c().func_149739_a()
                  + " from layers ["
                  + (worldHeightBlocks - 3)
                  + ", "
                  + (worldHeightBlocks - 1)
                  + "], bedrock="
                  + this.hasTopBedrock
            );
      }
   }

   @Override
   public void generateColumn(Chunk column) {
      this.biomes = this.world
         .func_72959_q()
         .func_76933_b(this.biomes, Coords.cubeToMinBlock(column.field_76635_g), Coords.cubeToMinBlock(column.field_76647_h), 16, 16);
      byte[] abyte = column.func_76605_m();

      for (int i = 0; i < abyte.length; i++) {
         abyte[i] = (byte)Biome.func_185362_a(this.biomes[i]);
      }
   }

   @Override
   public void recreateStructures(Chunk column) {
      this.vanilla.func_180514_a(column, column.field_76635_g, column.field_76647_h);
   }

   private Random getCubeSpecificRandom(int cubeX, int cubeY, int cubeZ) {
      Random rand = new Random(this.world.func_72905_C());
      rand.setSeed((long)(rand.nextInt() ^ cubeX));
      rand.setSeed((long)(rand.nextInt() ^ cubeZ));
      rand.setSeed((long)(rand.nextInt() ^ cubeY));
      return rand;
   }

   @Override
   public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
      try {
         WorldgenHangWatchdog.startWorldGen();
         this.tryInit(this.vanilla, this.world);
         CubePrimer primer = new CubePrimer();
         Random rand = new Random(this.world.func_72905_C());
         rand.setSeed((long)(rand.nextInt() ^ cubeX));
         rand.setSeed((long)(rand.nextInt() ^ cubeZ));
         if (cubeY >= 0 && cubeY < this.worldHeightCubes) {
            if (this.lastChunk.field_76635_g != cubeX || this.lastChunk.field_76647_h != cubeZ) {
               if (CubicChunksConfig.optimizedCompatibilityGenerator) {
                  try (ICubicWorldInternal.CompatGenerationScope ignored = ((ICubicWorldInternal.Server)this.world).doCompatibilityGeneration()) {
                     this.lastChunk = this.vanilla.func_185932_a(cubeX, cubeZ);
                     ChunkPrimer chunkPrimer = ((IColumnInternal)this.lastChunk).getCompatGenerationPrimer();
                     if (chunkPrimer == null) {
                        CubicChunks.LOGGER.error("Optimized compatibility generation failed, disabling...");
                        CubicChunksConfig.optimizedCompatibilityGenerator = false;
                     } else {
                        this.replaceBedrock(chunkPrimer, rand);
                     }
                  }
               } else {
                  this.lastChunk = this.vanilla.func_185932_a(cubeX, cubeZ);
               }
            }

            if (!this.optimizationHack) {
               this.optimizationHack = true;

               for (int y = this.worldHeightCubes - 1; y >= 0; y--) {
                  if (y != cubeY) {
                     ((ICubicWorld)this.world).getCubeFromCubeCoords(cubeX, y, cubeZ);
                  }
               }

               this.optimizationHack = false;
            }

            ChunkPrimer chunkPrimer = ((IColumnInternal)this.lastChunk).getCompatGenerationPrimer();
            if (chunkPrimer != null) {
               return new VanillaCompatibilityGenerator.CubePrimerWrapper(chunkPrimer, cubeY);
            }

            ExtendedBlockStorage storage = this.lastChunk.func_76587_i()[cubeY];
            if (((ICubicWorld)this.world).getMaxHeight() == 16) {
               if (cubeY != 0) {
                  storage = null;
               } else {
                  storage = this.lastChunk.func_76587_i()[4];
               }
            }

            if (storage != null && !storage.func_76663_a()) {
               for (int yx = 0; yx < 16; yx++) {
                  int blockY = Coords.localToBlock(cubeY, yx);

                  for (int z = 0; z < 16; z++) {
                     for (int x = 0; x < 16; x++) {
                        IBlockState state = storage.func_177485_a(x, yx, z);
                        if (state == Blocks.field_150357_h.func_176223_P()) {
                           if (yx < 8) {
                              state = this.extensionBlockBottom;
                           } else {
                              state = this.extensionBlockTop;
                           }

                           state = WorldGenUtils.getRandomBedrockReplacement(this.world, rand, state, blockY, 5, this.hasTopBedrock, this.hasBottomBedrock);
                           primer.setBlockState(x, yx, z, state);
                        } else {
                           state = WorldGenUtils.getRandomBedrockReplacement(this.world, rand, state, blockY, 5, this.hasTopBedrock, this.hasBottomBedrock);
                           primer.setBlockState(x, yx, z, state);
                        }
                     }
                  }
               }
            }
         } else {
            for (int yx = 0; yx < 16; yx++) {
               for (int z = 0; z < 16; z++) {
                  for (int xx = 0; xx < 16; xx++) {
                     IBlockState state = cubeY < 0 ? this.extensionBlockBottom : this.extensionBlockTop;
                     int blockY = Coords.localToBlock(cubeY, yx);
                     state = WorldGenUtils.getRandomBedrockReplacement(this.world, rand, state, blockY, 5, this.hasTopBedrock, this.hasBottomBedrock);
                     primer.setBlockState(xx, yx, z, state);
                  }
               }
            }
         }

         return primer;
      } finally {
         WorldgenHangWatchdog.endWorldGen();
      }
   }

   private void replaceBedrock(ChunkPrimer chunkPrimer, Random rand) {
      for (int y = 0; y < 8; y++) {
         this.replaceBedrockAtLayer(chunkPrimer, rand, y);
      }

      int startY = Coords.localToBlock(this.worldHeightCubes - 1, 8);
      int endY = Coords.cubeToMinBlock(this.worldHeightCubes);

      for (int y = startY; y < endY; y++) {
         this.replaceBedrockAtLayer(chunkPrimer, rand, y);
      }
   }

   private void replaceBedrockAtLayer(ChunkPrimer chunkPrimer, Random rand, int y) {
      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            IBlockState state = chunkPrimer.func_177856_a(x, y, z);
            if (state == Blocks.field_150357_h.func_176223_P()) {
               if (y < 64) {
                  chunkPrimer.func_177855_a(
                     x,
                     y,
                     z,
                     WorldGenUtils.getRandomBedrockReplacement(this.world, rand, this.extensionBlockBottom, y, 5, this.hasTopBedrock, this.hasBottomBedrock)
                  );
               } else {
                  chunkPrimer.func_177855_a(
                     x,
                     y,
                     z,
                     WorldGenUtils.getRandomBedrockReplacement(this.world, rand, this.extensionBlockTop, y, 5, this.hasTopBedrock, this.hasBottomBedrock)
                  );
               }
            }
         }
      }
   }

   @Override
   public void populate(ICube cube) {
      try {
         WorldgenHangWatchdog.startWorldGen();
         this.tryInit(this.vanilla, this.world);
         Random rand = this.getCubeSpecificRandom(cube.getX(), cube.getY(), cube.getZ());
         CubeGeneratorsRegistry.populateVanillaCubic(this.world, rand, cube);
         if (cube.getY() >= 0 && cube.getY() < this.worldHeightCubes) {
            if (cube.getY() >= 0 && cube.getY() < this.worldHeightCubes) {
               for (int y = this.worldHeightCubes - 1; y >= 0; y--) {
                  ((ICubicWorldInternal)this.world).getCubeFromCubeCoords(cube.getX(), y, cube.getZ()).setPopulated(true);
               }

               try {
                  CompatHandler.beforePopulate(this.world, this.vanilla);
                  this.vanilla.func_185931_b(cube.getX(), cube.getZ());
               } catch (IllegalArgumentException var13) {
                  StackTraceElement[] stack = var13.getStackTrace();
                  if (stack == null
                     || stack.length < 1
                     || !stack[0].getClassName().equals(Random.class.getName())
                     || !stack[0].getMethodName().equals("nextInt")) {
                     throw var13;
                  }

                  CubicChunks.LOGGER.error("Error while populating. Likely known mod issue, ignoring...", var13);
               } finally {
                  CompatHandler.afterPopulate(this.world);
               }

               this.applyModGenerators(cube.getX(), cube.getZ(), this.world, this.vanilla, this.world.func_72863_F());
            }

            return;
         }
      } finally {
         WorldgenHangWatchdog.endWorldGen();
      }
   }

   private void applyModGenerators(int x, int z, World world, IChunkGenerator vanillaGen, IChunkProvider provider) {
      List<IWorldGenerator> generators = IGameRegistry.getSortedGeneratorList();
      if (generators == null) {
         IGameRegistry.computeGenerators();
         generators = IGameRegistry.getSortedGeneratorList();

         assert generators != null;
      }

      long worldSeed = world.func_72905_C();
      Random fmlRandom = new Random(worldSeed);
      long xSeed = fmlRandom.nextLong() >> 3;
      long zSeed = fmlRandom.nextLong() >> 3;
      long chunkSeed = xSeed * (long)x + zSeed * (long)z ^ worldSeed;

      for (IWorldGenerator generator : generators) {
         fmlRandom.setSeed(chunkSeed);

         try {
            CompatHandler.beforeGenerate(world, generator);
            generator.generate(fmlRandom, x, z, world, vanillaGen, provider);
         } finally {
            CompatHandler.afterGenerate(world);
         }
      }
   }

   @Override
   public Box getFullPopulationRequirements(ICube cube) {
      return cube.getY() >= 0 && cube.getY() < this.worldHeightCubes
         ? new Box(-1, -cube.getY(), -1, 0, this.worldHeightCubes - cube.getY() - 1, 0)
         : NO_REQUIREMENT;
   }

   @Override
   public Box getPopulationPregenerationRequirements(ICube cube) {
      return cube.getY() >= 0 && cube.getY() < this.worldHeightCubes
         ? new Box(0, -cube.getY(), 0, 1, this.worldHeightCubes - cube.getY() - 1, 1)
         : NO_REQUIREMENT;
   }

   @Override
   public void recreateStructures(ICube cube) {
   }

   @Override
   public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
      return this.vanilla.func_177458_a(creatureType, pos);
   }

   @Override
   public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
      return this.vanilla.func_180513_a(this.world, name, pos, findUnexplored);
   }

   private static class CubePrimerWrapper extends CubePrimer {
      private final ChunkPrimer chunkPrimer;
      private final int cubeYBase;

      public CubePrimerWrapper(ChunkPrimer chunkPrimer, int cubeY) {
         super(null);
         this.chunkPrimer = chunkPrimer;
         this.cubeYBase = Coords.cubeToMinBlock(cubeY);
      }

      @Override
      public IBlockState getBlockState(int x, int y, int z) {
         return this.chunkPrimer.func_177856_a(x, y | this.cubeYBase, z);
      }

      @Override
      public void setBlockState(int x, int y, int z, @Nonnull IBlockState state) {
         this.chunkPrimer.func_177855_a(x, y | this.cubeYBase, z, state);
      }
   }
}
