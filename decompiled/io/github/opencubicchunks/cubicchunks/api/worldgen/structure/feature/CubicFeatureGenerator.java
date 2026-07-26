package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import java.util.Random;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubicFeatureGenerator implements ICubicFeatureGenerator {
   protected final int spacingBitCount;
   protected final int spacingBitCountY;
   private CubicFeatureData structureData;
   protected XYZMap<ICubicFeatureStart> structureMap = new XYZMap<>(0.5F, 1024);

   protected CubicFeatureGenerator(int spacingBitCount, int spacingBitCountY) {
      this.spacingBitCount = spacingBitCount;
      this.spacingBitCountY = spacingBitCountY;
   }

   @Override
   public void generate(World world, @Nullable CubePrimer cube, CubePos cubePos) {
      this.generate(world, cube, cubePos, this::generateFeature, 8, 8, this.spacingBitCount, this.spacingBitCountY);
   }

   protected synchronized void generateFeature(
      World world, Random rand, @Nullable CubePrimer cube, int structureX, int structureY, int structureZ, CubePos generatedCubePos
   ) {
      this.initializeStructureData(world);
      if (!this.structureMap.contains(structureX, structureY, structureZ)) {
         rand.nextInt();

         try {
            if (this.canSpawnStructureAtCoords(world, rand, structureX, structureY, structureZ)) {
               StructureStart start = this.getStructureStart(world, rand, structureX, structureY, structureZ);
               this.structureMap.put((ICubicFeatureStart)start);
               if (start.func_75069_d()) {
                  this.setStructureStart(structureX, structureY, structureZ, start);
               }
            }
         } catch (Throwable var11) {
            CrashReport report = CrashReport.func_85055_a(var11, "Exception preparing structure feature");
            CrashReportCategory category = report.func_85058_a("Feature being prepared");
            category.func_189529_a("Is feature chunk", () -> this.canSpawnStructureAtCoords(world, rand, structureX, structureY, structureZ) ? "True" : "False");
            category.func_71507_a("Chunk location", String.format("%d,%d,%d", structureX, structureY, structureZ));
            category.func_189529_a("Structure type", () -> this.getClass().getCanonicalName());
            throw new ReportedException(report);
         }
      }
   }

   @Override
   public synchronized boolean generateStructure(World world, Random rand, CubePos cubePos) {
      this.initializeStructureData(world);
      int centerX = Coords.cubeToCenterBlock(cubePos.getX());
      int centerY = Coords.cubeToCenterBlock(cubePos.getY());
      int centerZ = Coords.cubeToCenterBlock(cubePos.getZ());
      boolean generated = false;

      for (ICubicFeatureStart cubicStructureStart : this.structureMap) {
         StructureStart structStart = (StructureStart)cubicStructureStart;
         if (structStart.func_75069_d()
            && structStart.func_175788_a(cubePos.chunkPos())
            && structStart.func_75071_a()
               .func_78884_a(new StructureBoundingBox(centerX, centerY, centerZ, centerX + 16 - 1, centerY + 16 - 1, centerZ + 16 - 1))) {
            structStart.func_75068_a(world, rand, new StructureBoundingBox(centerX, centerY, centerZ, centerX + 16 - 1, centerY + 16 - 1, centerZ + 16 - 1));
            structStart.func_175787_b(cubePos.chunkPos());
            generated = true;
            this.setStructureStart(structStart.func_143019_e(), cubicStructureStart.getChunkPosY(), structStart.func_143018_f(), structStart);
         }
      }

      return generated;
   }

   @Override
   public boolean isInsideStructure(World world, BlockPos pos) {
      this.initializeStructureData(world);
      return this.getStructureAt(pos) != null;
   }

   @Nullable
   protected StructureStart getStructureAt(BlockPos pos) {
      for (ICubicFeatureStart cubicStructureStart : this.structureMap) {
         StructureStart start = (StructureStart)cubicStructureStart;
         if (start.func_75069_d() && start.func_75071_a().func_175898_b(pos)) {
            for (StructureComponent component : start.func_186161_c()) {
               if (component.func_74874_b().func_175898_b(pos)) {
                  return start;
               }
            }
         }
      }

      return null;
   }

   @Override
   public boolean isPositionInStructure(World world, BlockPos pos) {
      this.initializeStructureData(world);

      for (ICubicFeatureStart cubicStart : this.structureMap) {
         StructureStart start = (StructureStart)cubicStart;
         if (start.func_75069_d() && start.func_75071_a().func_175898_b(pos)) {
            return true;
         }
      }

      return false;
   }

   protected void initializeStructureData(World world) {
      if (this.structureData == null) {
         this.structureData = (CubicFeatureData)world.getPerWorldStorage().func_75742_a(CubicFeatureData.class, this.getStructureName());
         if (this.structureData == null) {
            this.structureData = new CubicFeatureData(this.getStructureName());
            world.getPerWorldStorage().func_75745_a(this.getStructureName(), this.structureData);
         } else {
            NBTTagCompound nbttagcompound = this.structureData.getTagCompound();

            for (String s : nbttagcompound.func_150296_c()) {
               NBTBase nbtbase = nbttagcompound.func_74781_a(s);
               if (nbtbase.func_74732_a() == 10) {
                  NBTTagCompound tag = (NBTTagCompound)nbtbase;
                  if (tag.func_74764_b("ChunkX") && tag.func_74764_b("ChunkY") && tag.func_74764_b("ChunkZ")) {
                     StructureStart structurestart = MapGenStructureIO.func_143035_a(tag, world);
                     if (structurestart != null) {
                        this.structureMap.put((ICubicFeatureStart)structurestart);
                     }
                  }
               }
            }
         }
      }
   }

   private void setStructureStart(int chunkX, int chunkY, int chunkZ, StructureStart start) {
      this.structureData.writeInstance(start.func_143021_a(chunkX, chunkZ), chunkX, chunkY, chunkZ);
      this.structureData.func_76185_a();
   }

   protected abstract boolean canSpawnStructureAtCoords(World var1, Random var2, int var3, int var4, int var5);

   protected abstract StructureStart getStructureStart(World var1, Random var2, int var3, int var4, int var5);
}
