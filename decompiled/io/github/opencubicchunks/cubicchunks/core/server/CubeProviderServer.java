package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Detainted;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.StartupQuery;
import net.minecraftforge.fml.common.registry.GameRegistry;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CubeProviderServer extends ChunkProviderServer implements ICubeProviderServer, ICubeProviderInternal.Server {
   @Nonnull
   private final EmptyColumn emptyColumn;
   @Nonnull
   private final BlankCube emptyCube;
   @Nonnull
   private final WorldServer worldServer;
   @Nonnull
   private final ICubeIO cubeIO;
   @Nonnull
   private final XYZMap<Cube> cubeMap = new XYZMap<>(0.7F, 8000);
   @Nonnull
   private final CubePrimer cubePrimer = new CubePrimer();
   @Nonnull
   private final ICubeGenerator cubeGen;
   @Nonnull
   private final Profiler profiler;
   private Chunk currentlyLoadingColumn;

   public CubeProviderServer(WorldServer worldServer, ICubeGenerator cubeGen) {
      super(worldServer, worldServer.func_72860_G().func_75763_a(worldServer.field_73011_w), worldServer.field_73011_w.func_186060_c());
      this.cubeGen = cubeGen;
      this.worldServer = worldServer;
      this.profiler = worldServer.field_72984_F;

      try {
         Path path = worldServer.func_72860_G().func_75765_b().toPath();
         if (worldServer.field_73011_w.getSaveFolder() != null) {
            path = path.resolve(worldServer.field_73011_w.getSaveFolder());
         }

         World overworld = worldServer.func_73046_m().func_130014_f_();
         WorldSavedCubicChunksData savedData = (WorldSavedCubicChunksData)overworld.getPerWorldStorage()
            .func_75742_a(WorldSavedCubicChunksData.class, "cubicChunksData");
         StorageFormatProviderBase format = (StorageFormatProviderBase)StorageFormatProviderBase.REGISTRY.getValue(savedData.storageFormat);
         if (format == null) {
            StartupQuery.notify("unsupported storage format \"" + savedData.storageFormat + '"');
            StartupQuery.abort();
         }

         this.cubeIO = new AsyncBatchingCubeIO(worldServer, format.provideStorage(worldServer, path));
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }

      this.emptyColumn = new EmptyColumn(worldServer, 0, 0);
      this.emptyCube = new BlankCube(this.emptyColumn);
   }

   @Detainted
   public void func_189549_a(Chunk chunk) {
   }

   @Detainted
   public void func_73240_a() {
   }

   @Nullable
   @Override
   public Chunk getLoadedColumn(int columnX, int columnZ) {
      Chunk chunk = (Chunk)this.field_73244_f.get(ChunkPos.func_77272_a(columnX, columnZ));
      return chunk == null ? this.currentlyLoadingColumn : chunk;
   }

   @Nullable
   @Deprecated
   public Chunk func_186026_b(int columnX, int columnZ) {
      return this.getLoadedColumn(columnX, columnZ);
   }

   @Nullable
   @Deprecated
   public Chunk func_186028_c(int columnX, int columnZ) {
      return this.loadChunk(columnX, columnZ, null);
   }

   @Nullable
   @Deprecated
   public Chunk loadChunk(int columnX, int columnZ, @Nullable Runnable runnable) {
      if (runnable == null) {
         return this.getColumn(columnX, columnZ, ICubeProviderServer.Requirement.LOAD);
      } else {
         this.asyncGetColumn(columnX, columnZ, ICubeProviderServer.Requirement.LOAD, col -> runnable.run());
         return null;
      }
   }

   @Override
   public Chunk provideColumn(int cubeX, int cubeZ) {
      return this.getColumn(cubeX, cubeZ, ICubeProviderServer.Requirement.GENERATE);
   }

   @Deprecated
   public Chunk func_186025_d(int cubeX, int cubeZ) {
      return this.provideColumn(cubeX, cubeZ);
   }

   public boolean func_186027_a(boolean alwaysTrue) {
      for (Cube cube : this.cubeMap) {
         if (cube.needsSaving()) {
            this.cubeIO.saveCube(cube);
         }
      }

      ObjectIterator var4 = this.field_73244_f.values().iterator();

      while (var4.hasNext()) {
         Chunk chunk = (Chunk)var4.next();
         if (chunk.func_76601_a(alwaysTrue)) {
            this.cubeIO.saveColumn(chunk);
         }
      }

      return true;
   }

   public boolean func_73156_b() {
      this.profiler.func_76320_a("providerTick");
      long i = System.currentTimeMillis();
      Random rand = this.field_73251_h.field_73012_v;
      PlayerCubeMap playerCubeMap = (PlayerCubeMap)this.field_73251_h.func_184164_w();
      Iterator<Cube> watchersIterator = playerCubeMap.getCubeIterator();
      BooleanSupplier tickFaster = () -> System.currentTimeMillis() - i > 40L;

      while (watchersIterator.hasNext()) {
         watchersIterator.next().tickCubeServer(tickFaster, rand);
      }

      this.profiler.func_76319_b();
      return false;
   }

   public String func_73148_d() {
      return "CubeProviderServer: " + this.field_73244_f.size() + " columns, " + this.cubeMap.getSize() + " cubes";
   }

   public List<SpawnListEntry> func_177458_a(EnumCreatureType type, BlockPos pos) {
      return this.cubeGen.getPossibleCreatures(type, pos);
   }

   @Nullable
   public BlockPos func_180513_a(World worldIn, String name, BlockPos pos, boolean findUnexplored) {
      return this.cubeGen.getClosestStructure(name, pos, findUnexplored);
   }

   public boolean func_73149_a(int cubeX, int cubeZ) {
      return this.field_73244_f.get(ChunkPos.func_77272_a(cubeX, cubeZ)) != null;
   }

   public boolean func_193413_a(World p_193413_1_, String p_193413_2_, BlockPos p_193413_3_) {
      return false;
   }

   @Override
   public Cube getCube(int cubeX, int cubeY, int cubeZ) {
      return this.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.GENERATE);
   }

   @Override
   public Cube getCube(CubePos coords) {
      return this.getCube(coords.getX(), coords.getY(), coords.getZ());
   }

   @Nullable
   @Override
   public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
      return this.cubeMap.get(cubeX, cubeY, cubeZ);
   }

   @Nullable
   @Override
   public Cube getLoadedCube(CubePos coords) {
      return this.getLoadedCube(coords.getX(), coords.getY(), coords.getZ());
   }

   public void asyncGetCube(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req, Consumer<Cube> callback) {
      Cube cube = this.getLoadedCube(cubeX, cubeY, cubeZ);
      if (req != ICubeProviderServer.Requirement.GET_CACHED && (cube == null || req.compareTo(ICubeProviderServer.Requirement.GENERATE) > 0)) {
         if (cube == null) {
            AsyncWorldIOExecutor.queueCubeLoad(this.worldServer, this.cubeIO, this, cubeX, cubeY, cubeZ, loaded -> {
               Chunk col = this.getLoadedColumn(cubeX, cubeZ);
               if (col != null) {
                  assert !col.func_76621_g();

                  this.onCubeLoaded(loaded, col);
                  loaded = this.postCubeLoadAttempt(cubeX, cubeY, cubeZ, loaded, col, req, false);
               }

               callback.accept(loaded);
            });
         }
      } else {
         callback.accept(cube);
      }
   }

   @Nullable
   public Cube getCube(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req) {
      return this.getCube(cubeX, cubeY, cubeZ, req, false);
   }

   @Nullable
   public Cube getCubeNow(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req) {
      return this.getCube(cubeX, cubeY, cubeZ, req, true);
   }

   @Nullable
   private Cube getCube(int cubeX, int cubeY, int cubeZ, ICubeProviderServer.Requirement req, boolean forceNow) {
      Cube cube = this.getLoadedCube(cubeX, cubeY, cubeZ);
      if (req != ICubeProviderServer.Requirement.GET_CACHED && (cube == null || req.compareTo(ICubeProviderServer.Requirement.GENERATE) > 0)) {
         Chunk column = this.getColumn(cubeX, cubeZ, req, forceNow);
         if (column == null) {
            return cube;
         } else if (column.func_76621_g()) {
            return this.emptyCube;
         } else {
            if (cube == null) {
               cube = this.getLoadedCube(cubeX, cubeY, cubeZ);
            }

            if (cube == null) {
               cube = AsyncWorldIOExecutor.syncCubeLoad(this.worldServer, this.cubeIO, this, cubeX, cubeY, cubeZ);
               this.onCubeLoaded(cube, column);
            }

            return this.postCubeLoadAttempt(cubeX, cubeY, cubeZ, cube, column, req, forceNow);
         }
      } else {
         return cube;
      }
   }

   @Override
   public boolean isCubeGenerated(int cubeX, int cubeY, int cubeZ) {
      return this.getLoadedCube(cubeX, cubeY, cubeZ) != null || this.cubeIO.cubeExists(cubeX, cubeY, cubeZ);
   }

   private void onCubeLoaded(@Nullable Cube cube, Chunk column) {
      if (cube != null) {
         this.cubeMap.put(cube);
         if (!((IColumn)column).getLoadedCubes().contains(cube)) {
            ((IColumn)column).addCube(cube);
            cube.onLoad();
         }
      }
   }

   @Nullable
   private Cube postCubeLoadAttempt(int cubeX, int cubeY, int cubeZ, @Nullable Cube cube, Chunk column, ICubeProviderServer.Requirement req, boolean forceNow) {
      if (cube == null) {
         cube = this.getLoadedCube(cubeX, cubeY, cubeZ);
      }

      if (req == ICubeProviderServer.Requirement.LOAD) {
         return cube;
      } else if (req == ICubeProviderServer.Requirement.GENERATE && cube != null) {
         return cube;
      } else {
         if (cube == null) {
            if (!forceNow && this.cubeGen.pollAsyncCubeGenerator(cubeX, cubeY, cubeZ) != ICubeGenerator.GeneratorReadyState.READY) {
               return this.emptyCube;
            }

            cube = this.generateCube(cubeX, cubeY, cubeZ, column, forceNow).orElse(null);
            if (cube == null) {
               return this.emptyCube;
            }

            if (req == ICubeProviderServer.Requirement.GENERATE) {
               return cube;
            }
         }

         if (!cube.isFullyPopulated()) {
            if (!forceNow && this.cubeGen.pollAsyncCubePopulator(cubeX, cubeY, cubeZ) != ICubeGenerator.GeneratorReadyState.READY) {
               return this.emptyCube;
            }

            if (!this.populateCube(cube, forceNow)) {
               return cube;
            }

            if (req == ICubeProviderServer.Requirement.POPULATE) {
               return cube;
            }
         }

         if (!cube.isInitialLightingDone() || !cube.isSurfaceTracked()) {
            this.calculateDiffuseSkylight(cube);
         }

         if (!cube.isSurfaceTracked()) {
            cube.trackSurface();
         }

         return cube;
      }
   }

   private Optional<Cube> generateCube(int cubeX, int cubeY, int cubeZ, Chunk column, boolean forceGenerate) {
      return this.cubeGen.tryGenerateCube(cubeX, cubeY, cubeZ, this.cubePrimer, forceGenerate).map(primer -> {
         Cube cube = new Cube(column, cubeY, primer);
         this.onCubeLoaded(cube, column);
         if (primer == this.cubePrimer) {
            primer.reset();
         }

         return cube;
      });
   }

   private boolean populateCube(Cube cube, boolean forceNow) {
      int cubeX = cube.getX();
      int cubeY = cube.getY();
      int cubeZ = cube.getZ();
      Box fullPopulation = this.cubeGen.getFullPopulationRequirements(cube);
      if (CubicChunksConfig.useVanillaChunkWorldGenerators && cube.getY() >= 0 && cube.getY() < 16) {
         fullPopulation = new Box(0, -cube.getY(), 0, 0, 16 - cube.getY() - 1, 0).add(fullPopulation);
      }

      boolean success = fullPopulation.allMatch((x, y, z) -> {
         Cube fullPopulationCube = this.getCube(x + cubeX, y + cubeY, z + cubeZ);
         Box newBox = this.cubeGen.getPopulationPregenerationRequirements(fullPopulationCube);
         if (CubicChunksConfig.useVanillaChunkWorldGenerators && cube.getY() >= 0 && cube.getY() < 16) {
            newBox = new Box(0, -cube.getY(), 0, 0, 16 - cube.getY() - 1, 0).add(newBox);
         }

         boolean generated = newBox.allMatch((nx, ny, nz) -> {
            int genX = cubeX + x + nx;
            int genY = cubeY + y + ny;
            int genZ = cubeZ + z + nz;
            return !(this.getCube(genX, genY, genZ, ICubeProviderServer.Requirement.GENERATE, forceNow) instanceof BlankCube);
         });
         if (!generated) {
            return false;
         } else {
            if (!fullPopulationCube.isPopulated()) {
               this.cubeGen.populate(fullPopulationCube);
               fullPopulationCube.setPopulated(true);
            }

            return true;
         }
      });
      if (!success) {
         return false;
      } else {
         if (CubicChunksConfig.useVanillaChunkWorldGenerators) {
            Box.Mutable box = fullPopulation.asMutable();
            box.setY1(0);
            box.setY2(0);
            box.forEachPoint(
               (x, y, z) -> GameRegistry.generateWorld(
                     cube.getX() + x, cube.getZ() + z, this.field_73251_h, this.field_186029_c, this.field_73251_h.func_72863_F()
                  )
            );
         }

         cube.setFullyPopulated(true);
         return true;
      }
   }

   private void calculateDiffuseSkylight(Cube cube) {
      if (LightingManager.NO_SUNLIGHT_PROPAGATION) {
         cube.setInitialLightingDone(true);
      } else {
         int cubeX = cube.getX();
         int cubeY = cube.getY();
         int cubeZ = cube.getZ();

         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               for (int y = 1; y >= -1; y--) {
                  if (x != 0 || y != 0 || z != 0) {
                     this.getCube(x + cubeX, y + cubeY, z + cubeZ);
                  }
               }
            }
         }

         ((ICubicWorldInternal.Server)this.worldServer).getFirstLightProcessor().diffuseSkylight(cube);
         cube.setInitialLightingDone(true);
      }
   }

   public void asyncGetColumn(int columnX, int columnZ, ICubeProviderServer.Requirement req, Consumer<Chunk> callback) {
      Chunk column = this.getLoadedColumn(columnX, columnZ);
      if (column == null && req != ICubeProviderServer.Requirement.GET_CACHED) {
         AsyncWorldIOExecutor.queueColumnLoad(this.worldServer, this.cubeIO, columnX, columnZ, col -> {
            col = this.postProcessColumn(columnX, columnZ, col, req, false);
            callback.accept(col);
         }, col -> this.currentlyLoadingColumn = col);
      } else {
         callback.accept(column);
      }
   }

   @Nullable
   @Override
   public Chunk getColumn(int columnX, int columnZ, ICubeProviderServer.Requirement req) {
      return this.getColumn(columnX, columnZ, req, false);
   }

   @Nullable
   private Chunk getColumn(int columnX, int columnZ, ICubeProviderServer.Requirement req, boolean forceNow) {
      Chunk column = this.getLoadedColumn(columnX, columnZ);
      if (column == null && req != ICubeProviderServer.Requirement.GET_CACHED) {
         column = AsyncWorldIOExecutor.syncColumnLoad(this.worldServer, this.cubeIO, columnX, columnZ, col -> this.currentlyLoadingColumn = col);
         return this.postProcessColumn(columnX, columnZ, column, req, forceNow);
      } else {
         return column;
      }
   }

   @Nullable
   private Chunk postProcessColumn(int columnX, int columnZ, @Nullable Chunk column, ICubeProviderServer.Requirement req, boolean force) {
      Chunk loaded = this.getLoadedColumn(columnX, columnZ);
      if (loaded != null) {
         if (column != null && loaded != column) {
            throw new IllegalStateException("Duplicate column at " + columnX + ", " + columnZ + "!");
         } else {
            return loaded;
         }
      } else if (column != null) {
         this.field_73244_f.put(ChunkPos.func_77272_a(columnX, columnZ), column);
         column.func_177432_b(this.worldServer.func_82737_E());
         column.func_76631_c();
         return column;
      } else if (req == ICubeProviderServer.Requirement.LOAD) {
         return null;
      } else if (!force && this.cubeGen.pollAsyncColumnGenerator(columnX, columnZ) != ICubeGenerator.GeneratorReadyState.READY) {
         return this.emptyColumn;
      } else {
         column = this.cubeGen.tryGenerateColumn(this.field_73251_h, columnX, columnZ, new ChunkPrimer(), force).orElse(null);
         if (column == null) {
            return this.emptyColumn;
         } else {
            this.field_73244_f.put(ChunkPos.func_77272_a(columnX, columnZ), column);
            column.func_177432_b(this.worldServer.func_82737_E());
            column.func_76631_c();
            return column;
         }
      }
   }

   public String dumpLoadedCubes() {
      StringBuilder sb = new StringBuilder(10000).append("\n");
      ObjectIterator var2 = this.field_73244_f.values().iterator();

      while (var2.hasNext()) {
         Chunk chunk = (Chunk)var2.next();
         if (chunk == null) {
            sb.append("column = null\n");
         } else {
            sb.append("Column[").append(chunk.field_76635_g).append(", ").append(chunk.field_76647_h).append("] {");
            boolean isFirst = true;

            for (ICube cube : ((IColumn)chunk).getLoadedCubes()) {
               if (!isFirst) {
                  sb.append(", ");
               }

               isFirst = false;
               if (cube == null) {
                  sb.append("cube = null");
               } else {
                  sb.append("Cube[").append(cube.getY()).append("]");
               }
            }

            sb.append("\n");
         }
      }

      return sb.toString();
   }

   @Nonnull
   @Override
   public ICubeIO getCubeIO() {
      return this.cubeIO;
   }

   Iterator<Cube> cubesIterator() {
      return this.cubeMap.iterator();
   }

   Iterator<Chunk> columnsIterator() {
      return this.field_73244_f.values().iterator();
   }

   boolean tryUnloadCube(Cube cube) {
      if (ForgeChunkManager.getPersistentChunksFor(this.field_73251_h).containsKey(cube.getColumn().func_76632_l())) {
         return false;
      } else if (!cube.getTickets().canUnload()) {
         return false;
      } else {
         cube.onUnload();
         if (cube.needsSaving()) {
            this.cubeIO.saveCube(cube);
         }

         if (((IColumn)cube.getColumn()).removeCube(cube.getY()) == null) {
            throw new RuntimeException();
         } else {
            return true;
         }
      }
   }

   boolean tryUnloadColumn(Chunk column) {
      if (ForgeChunkManager.getPersistentChunksFor(this.field_73251_h).containsKey(column.func_76632_l())) {
         return false;
      } else if (((IColumn)column).hasLoadedCubes()) {
         return false;
      } else if (this.field_73251_h.func_184164_w().func_152621_a(column.field_76635_g, column.field_76647_h)) {
         return false;
      } else if (!AsyncWorldIOExecutor.canDropColumn(this.worldServer, column.field_76635_g, column.field_76647_h)) {
         return false;
      } else {
         column.field_189550_d = true;
         column.func_76623_d();
         if (column.func_76601_a(true)) {
            this.cubeIO.saveColumn(column);
         }

         return true;
      }
   }

   public ICubeGenerator getCubeGenerator() {
      return this.cubeGen;
   }

   public int getLoadedCubeCount() {
      return this.cubeMap.getSize();
   }
}
