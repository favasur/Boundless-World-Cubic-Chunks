package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent.Save;
import net.minecraftforge.fml.common.FMLCommonHandler;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class IONbtWriter {
   IONbtWriter() {
   }

   static byte[] writeNbtBytes(NBTTagCompound nbt) throws IOException {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      CompressedStreamTools.func_74799_a(nbt, buf);
      return buf.toByteArray();
   }

   static NBTTagCompound write(Chunk column) {
      NBTTagCompound columnNbt = new NBTTagCompound();
      NBTTagCompound level = new NBTTagCompound();
      columnNbt.func_74782_a("Level", level);
      columnNbt.func_74768_a("DataVersion", FMLCommonHandler.instance().getDataFixer().field_188262_d);
      FMLCommonHandler.instance().getDataFixer().writeVersionData(columnNbt);
      writeBaseColumn(column, level);
      writeBiomes(column, level);
      writeOpacityIndex(column, level);
      MinecraftForge.EVENT_BUS.post(new Save(column, columnNbt));
      return columnNbt;
   }

   static NBTTagCompound write(Cube cube) {
      NBTTagCompound cubeNbt = new NBTTagCompound();
      NBTTagCompound level = new NBTTagCompound();
      cubeNbt.func_74782_a("Level", level);
      cubeNbt.func_74768_a("DataVersion", FMLCommonHandler.instance().getDataFixer().field_188262_d);
      FMLCommonHandler.instance().getDataFixer().writeVersionData(cubeNbt);
      writeBaseCube(cube, level);
      writeBlocks(cube, level);
      writeEntities(cube, level);
      writeTileEntities(cube, level);
      writeScheduledTicks(cube, level);
      writeLightingInfo(cube, level);
      writeBiomes(cube, level);
      writeModData(cube, cubeNbt);
      return cubeNbt;
   }

   private static void writeBaseColumn(Chunk column, NBTTagCompound nbt) {
      nbt.func_74768_a("x", column.field_76635_g);
      nbt.func_74768_a("z", column.field_76647_h);
      nbt.func_74774_a("v", (byte)1);
      nbt.func_74772_a("InhabitedTime", column.func_177416_w());
      if (column.getCapabilities() != null) {
         try {
            nbt.func_74782_a("ForgeCaps", column.getCapabilities().serializeNBT());
         } catch (Exception var3) {
            CubicChunks.LOGGER
               .error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", var3);
         }
      }
   }

   private static void writeBiomes(Chunk column, NBTTagCompound nbt) {
      nbt.func_74773_a("Biomes", column.func_76605_m());
   }

   private static void writeOpacityIndex(Chunk column, NBTTagCompound nbt) {
      IHeightMap hmap = ((IColumn)column).getOpacityIndex();
      if (hmap instanceof ServerHeightMap) {
         nbt.func_74773_a("OpacityIndex", ((ServerHeightMap)hmap).getData());
      } else {
         nbt.func_74773_a("OpacityIndexClient", ((ClientHeightMap)hmap).getData());
      }
   }

   private static void writeBaseCube(Cube cube, NBTTagCompound cubeNbt) {
      cubeNbt.func_74774_a("v", (byte)1);
      cubeNbt.func_74768_a("x", cube.getX());
      cubeNbt.func_74768_a("y", cube.getY());
      cubeNbt.func_74768_a("z", cube.getZ());
      cubeNbt.func_74757_a("populated", cube.isPopulated());
      cubeNbt.func_74757_a("isSurfaceTracked", cube.isSurfaceTracked());
      cubeNbt.func_74757_a("fullyPopulated", cube.isFullyPopulated());
      cubeNbt.func_74768_a("initLightVersion", 1);
      cubeNbt.func_74757_a("initLightDone", cube.isInitialLightingDone());
      if (cube.getCapabilities() != null) {
         try {
            cubeNbt.func_74782_a("ForgeCaps", cube.getCapabilities().serializeNBT());
         } catch (Exception var3) {
            CubicChunks.LOGGER
               .error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", var3);
         }
      }
   }

   private static void writeBlocks(Cube cube, NBTTagCompound cubeNbt) {
      ExtendedBlockStorage ebs = cube.getStorage();
      if (ebs != null) {
         NBTTagList sectionList = new NBTTagList();
         NBTTagCompound section = new NBTTagCompound();
         sectionList.func_74742_a(section);
         cubeNbt.func_74782_a("Sections", sectionList);
         byte[] abyte = new byte[4096];
         NibbleArray data = new NibbleArray();
         NibbleArray add = null;
         NibbleArray add2neid = null;

         for (int i = 0; i < 4096; i++) {
            int x = i & 15;
            int y = i >> 8 & 15;
            int z = i >> 4 & 15;
            int id = Block.field_176229_d.func_148747_b(ebs.func_186049_g().func_186016_a(x, y, z));
            int in1 = id >> 12 & 15;
            int in2 = id >> 16 & 15;
            if (in1 != 0) {
               if (add == null) {
                  add = new NibbleArray();
               }

               add.func_177482_a(i, in1);
            }

            if (in2 != 0) {
               if (add2neid == null) {
                  add2neid = new NibbleArray();
               }

               add2neid.func_177482_a(i, in2);
            }

            abyte[i] = (byte)(id >> 4 & 0xFF);
            data.func_177482_a(i, id & 15);
         }

         section.func_74773_a("Blocks", abyte);
         section.func_74773_a("Data", data.func_177481_a());
         if (add != null) {
            section.func_74773_a("Add", add.func_177481_a());
         }

         if (add2neid != null) {
            section.func_74773_a("Add2", add2neid.func_177481_a());
         }

         section.func_74773_a("BlockLight", ebs.func_76661_k().func_177481_a());
         if (cube.getWorld().field_73011_w.func_191066_m()) {
            section.func_74773_a("SkyLight", ebs.func_76671_l().func_177481_a());
         }
      }
   }

   private static void writeEntities(Cube cube, NBTTagCompound cubeNbt) {
      cube.getEntityContainer()
         .writeToNbt(
            cubeNbt,
            "Entities",
            entity -> {
               int cubeX = Coords.getCubeXForEntity(entity);
               int cubeY = Coords.getCubeYForEntity(entity);
               int cubeZ = Coords.getCubeZForEntity(entity);
               if (cubeX != cube.getX() || cubeY != cube.getY() || cubeZ != cube.getZ()) {
                  CubicChunks.LOGGER
                     .warn(
                        String.format(
                           "Saved entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)! Entity thinks its in (%d,%d,%d)",
                           entity.getClass().getName(),
                           cubeX,
                           cubeY,
                           cubeZ,
                           cube.getX(),
                           cube.getY(),
                           cube.getZ(),
                           entity.field_70176_ah,
                           entity.field_70162_ai,
                           entity.field_70164_aj
                        )
                     );
               }
            }
         );
   }

   private static void writeTileEntities(Cube cube, NBTTagCompound cubeNbt) {
      NBTTagList nbtTileEntities = new NBTTagList();
      cubeNbt.func_74782_a("TileEntities", nbtTileEntities);

      for (TileEntity blockEntity : cube.getTileEntityMap().values()) {
         NBTTagCompound nbtTileEntity = new NBTTagCompound();
         blockEntity.func_189515_b(nbtTileEntity);
         nbtTileEntities.func_74742_a(nbtTileEntity);
      }
   }

   private static void writeScheduledTicks(Cube cube, NBTTagCompound cubeNbt) {
      Iterable<NextTickListEntry> scheduledTicks = getScheduledTicks(cube);
      long time = cube.getWorld().func_82737_E();
      NBTTagList nbtTicks = new NBTTagList();
      cubeNbt.func_74782_a("TileTicks", nbtTicks);

      for (NextTickListEntry scheduledTick : scheduledTicks) {
         NBTTagCompound nbtScheduledTick = new NBTTagCompound();
         ResourceLocation resourcelocation = (ResourceLocation)Block.field_149771_c.func_177774_c(scheduledTick.func_151351_a());
         nbtScheduledTick.func_74778_a("i", resourcelocation.toString());
         nbtScheduledTick.func_74768_a("x", scheduledTick.field_180282_a.func_177958_n());
         nbtScheduledTick.func_74768_a("y", scheduledTick.field_180282_a.func_177956_o());
         nbtScheduledTick.func_74768_a("z", scheduledTick.field_180282_a.func_177952_p());
         nbtScheduledTick.func_74768_a("t", (int)(scheduledTick.field_77180_e - time));
         nbtScheduledTick.func_74768_a("p", scheduledTick.field_82754_f);
         nbtTicks.func_74742_a(nbtScheduledTick);
      }
   }

   private static void writeLightingInfo(Cube cube, NBTTagCompound cubeNbt) {
      NBTTagCompound lightingInfo = new NBTTagCompound();
      cubeNbt.func_74782_a("LightingInfo", lightingInfo);
      int[] lastHeightmap = cube.getColumn().func_177445_q();
      lightingInfo.func_74783_a("LastHeightMap", lastHeightmap);
      byte edgeNeedSkyLightUpdate = 0;
      LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = cube.getCubeLightUpdateInfo();
      if (cubeLightUpdateInfo != null) {
         for (EnumFacing enumFacing : cubeLightUpdateInfo.edgeNeedSkyLightUpdate) {
            edgeNeedSkyLightUpdate = (byte)(edgeNeedSkyLightUpdate | 1 << enumFacing.ordinal());
         }
      }

      lightingInfo.func_74774_a("EdgeNeedSkyLightUpdate", edgeNeedSkyLightUpdate);
   }

   private static void writeModData(Cube cube, NBTTagCompound level) {
      MinecraftForge.EVENT_BUS.post(new CubeDataEvent.Save(cube, level));
   }

   private static void writeBiomes(Cube cube, NBTTagCompound nbt) {
      byte[] biomes = cube.getBiomeArray();
      if (biomes != null) {
         nbt.func_74773_a("Biomes", biomes);
      }
   }

   private static List<NextTickListEntry> getScheduledTicks(Cube cube) {
      ArrayList<NextTickListEntry> out = new ArrayList<>();
      if (!(cube.getWorld() instanceof WorldServer)) {
         return out;
      } else {
         WorldServer worldServer = cube.getWorld();
         out.addAll(((ICubicWorldInternal.Server)worldServer).getScheduledTicks().getForCube(cube.getCoords()));
         out.addAll(((ICubicWorldInternal.Server)worldServer).getThisTickScheduledTicks().getForCube(cube.getCoords()));
         return out;
      }
   }
}
