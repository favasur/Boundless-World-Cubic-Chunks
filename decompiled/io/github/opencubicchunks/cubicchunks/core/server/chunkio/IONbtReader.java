package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IONbtReader {
   public IONbtReader() {
   }

   @Nullable
   static Chunk readColumn(World world, int x, int z, NBTTagCompound nbt) {
      NBTTagCompound level = nbt.func_74775_l("Level");
      Chunk column = readBaseColumn(world, x, z, level);
      if (column == null) {
         return null;
      } else {
         readBiomes(level, column);
         readOpacityIndex(level, column);
         column.func_177427_f(false);
         return column;
      }
   }

   @Nullable
   private static Chunk readBaseColumn(World world, int x, int z, NBTTagCompound nbt) {
      byte version = nbt.func_74771_c("v");
      if (version != 1) {
         throw new IllegalArgumentException(String.format("Column has wrong version: %d", version));
      } else {
         int xCheck = nbt.func_74762_e("x");
         int zCheck = nbt.func_74762_e("z");
         if (xCheck == x && zCheck == z) {
            Chunk column = new Chunk(world, x, z);
            column.func_177415_c(nbt.func_74763_f("InhabitedTime"));
            if (column.getCapabilities() != null && nbt.func_74764_b("ForgeCaps")) {
               column.getCapabilities().deserializeNBT(nbt.func_74775_l("ForgeCaps"));
            }

            return column;
         } else {
            CubicChunks.LOGGER.warn(String.format("Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck));
            return null;
         }
      }
   }

   private static void readBiomes(NBTTagCompound nbt, Chunk column) {
      System.arraycopy(nbt.func_74770_j("Biomes"), 0, column.func_76605_m(), 0, 256);
   }

   private static void readOpacityIndex(NBTTagCompound nbt, Chunk chunk) {
      IHeightMap hmap = ((IColumn)chunk).getOpacityIndex();
      if (hmap instanceof ServerHeightMap) {
         ((ServerHeightMap)hmap).readData(nbt.func_74770_j("OpacityIndex"));
      } else {
         ((ClientHeightMap)hmap).setData(nbt.func_74770_j("OpacityIndexClient"));
      }
   }

   @Nullable
   static Cube readCubeAsyncPart(Chunk column, int cubeX, int cubeY, int cubeZ, NBTTagCompound nbt) {
      if (column.field_76635_g == cubeX && column.field_76647_h == cubeZ) {
         World world = column.func_177412_p();
         NBTTagCompound level = nbt.func_74775_l("Level");
         Cube cube = readBaseCube(column, cubeX, cubeY, cubeZ, level, world);
         if (cube == null) {
            return null;
         } else {
            readBiomes(cube, level);
            readBlocks(level, world, cube);
            return cube;
         }
      } else {
         throw new IllegalArgumentException(
            String.format("Invalid column (%d, %d) for cube at (%d, %d, %d)", column.field_76635_g, column.field_76647_h, cubeX, cubeY, cubeZ)
         );
      }
   }

   static void readCubeSyncPart(Cube cube, World world, NBTTagCompound nbt) {
      ((IColumn)cube.getColumn()).preCacheCube(cube);
      NBTTagCompound level = nbt.func_74775_l("Level");
      readEntities(level, world, cube);
      readTileEntities(level, world, cube);
      readScheduledBlockTicks(level, world);
      readLightingInfo(cube, level, world);
      cube.markSaved();
   }

   @Nullable
   private static Cube readBaseCube(Chunk column, int cubeX, int cubeY, int cubeZ, NBTTagCompound nbt, World world) {
      byte version = nbt.func_74771_c("v");
      if (version != 1) {
         throw new IllegalArgumentException(String.format("Cube at CubePos:(%d, %d, %d), has wrong version! %d", cubeX, cubeY, cubeZ, version));
      } else {
         int xCheck = nbt.func_74762_e("x");
         int yCheck = nbt.func_74762_e("y");
         int zCheck = nbt.func_74762_e("z");
         if (xCheck == cubeX && yCheck == cubeY && zCheck == cubeZ) {
            assert cubeX == column.field_76635_g && cubeZ == column.field_76647_h : String.format(
               "Cube is corrupted! Cube (%d,%d,%d) does not match column (%d,%d).", cubeX, cubeY, cubeZ, column.field_76647_h, column.field_76647_h
            );

            Cube cube = new Cube(column, cubeY);
            cube.setPopulated(nbt.func_74767_n("populated"));
            cube.setSurfaceTracked(nbt.func_74767_n("isSurfaceTracked"));
            cube.setFullyPopulated(nbt.func_74767_n("fullyPopulated"));
            int lightVersion = nbt.func_74762_e("initLightVersion");
            cube.setInitialLightingDone((!CubicChunksConfig.updateKnownBrokenLightingOnLoad || lightVersion >= 1) && nbt.func_74767_n("initLightDone"));
            if (cube.getCapabilities() != null && nbt.func_74764_b("ForgeCaps")) {
               cube.getCapabilities().deserializeNBT(nbt.func_74775_l("ForgeCaps"));
            }

            return cube;
         } else {
            CubicChunks.LOGGER
               .error(
                  String.format(
                     "Cube is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d). Cube will be regenerated.", cubeX, cubeY, cubeZ, xCheck, yCheck, zCheck
                  )
               );
            return null;
         }
      }
   }

   private static void readBlocks(NBTTagCompound nbt, World world, Cube cube) {
      boolean isEmpty = !nbt.func_74764_b("Sections");
      if (!isEmpty) {
         NBTTagList sectionList = nbt.func_150295_c("Sections", 10);
         nbt = sectionList.func_150305_b(0);
         ExtendedBlockStorage ebs = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()), cube.getWorld().field_73011_w.func_191066_m());
         byte[] abyte = nbt.func_74770_j("Blocks");
         NibbleArray data = new NibbleArray(nbt.func_74770_j("Data"));
         NibbleArray add = nbt.func_150297_b("Add", 7) ? new NibbleArray(nbt.func_74770_j("Add")) : null;
         NibbleArray add2neid = nbt.func_150297_b("Add2", 7) ? new NibbleArray(nbt.func_74770_j("Add2")) : null;

         for (int i = 0; i < 4096; i++) {
            int x = i & 15;
            int y = i >> 8 & 15;
            int z = i >> 4 & 15;
            int toAdd = add == null ? 0 : add.func_177480_a(i);
            toAdd = toAdd & 15 | (add2neid == null ? 0 : add2neid.func_177480_a(i) << 4);
            int id = toAdd << 12 | (abyte[i] & 255) << 4 | data.func_177480_a(i);
            ebs.func_186049_g().func_186013_a(x, y, z, (IBlockState)Block.field_176229_d.func_148745_a(id));
         }

         ebs.func_76659_c(new NibbleArray(nbt.func_74770_j("BlockLight")));
         if (world.field_73011_w.func_191066_m()) {
            ebs.func_76666_d(new NibbleArray(nbt.func_74770_j("SkyLight")));
         }

         ebs.func_76672_e();
         cube.setStorage(ebs);
      }
   }

   private static void readEntities(NBTTagCompound nbt, World world, Cube cube) {
      cube.getEntityContainer()
         .readFromNbt(
            nbt,
            "Entities",
            world,
            entity -> {
               int entityCubeX = Coords.getCubeXForEntity(entity);
               int entityCubeY = Coords.getCubeYForEntity(entity);
               int entityCubeZ = Coords.getCubeZForEntity(entity);
               if (entityCubeX != cube.getX() || entityCubeY != cube.getY() || entityCubeZ != cube.getZ()) {
                  CubicChunks.LOGGER
                     .warn(
                        String.format(
                           "Loaded entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)!",
                           entity.getClass().getName(),
                           entityCubeX,
                           entityCubeY,
                           entityCubeZ,
                           cube.getX(),
                           cube.getY(),
                           cube.getZ()
                        )
                     );
               }

               entity.field_70175_ag = true;
               entity.field_70176_ah = cube.getX();
               entity.field_70162_ai = cube.getY();
               entity.field_70164_aj = cube.getZ();
            }
         );
   }

   private static void readTileEntities(NBTTagCompound nbt, World world, Cube cube) {
      NBTTagList nbtTileEntities = nbt.func_150295_c("TileEntities", 10);

      for (int i = 0; i < nbtTileEntities.func_74745_c(); i++) {
         NBTTagCompound nbtTileEntity = nbtTileEntities.func_150305_b(i);
         TileEntity blockEntity = TileEntity.func_190200_a(world, nbtTileEntity);
         if (blockEntity != null) {
            if (!cube.getCoords().containsBlock(blockEntity.func_174877_v())) {
               CubicChunks.LOGGER.warn("TileEntity " + blockEntity + " is not in cube at " + cube.getCoords() + ", tile entity will be skipped");
            } else {
               cube.addTileEntity(blockEntity);
            }
         }
      }
   }

   private static void readScheduledBlockTicks(NBTTagCompound nbt, World world) {
      if (world instanceof WorldServer) {
         NBTTagList nbtScheduledTicks = nbt.func_150295_c("TileTicks", 10);

         for (int i = 0; i < nbtScheduledTicks.func_74745_c(); i++) {
            NBTTagCompound nbtScheduledTick = nbtScheduledTicks.func_150305_b(i);
            Block block;
            if (nbtScheduledTick.func_150297_b("i", 8)) {
               block = Block.func_149684_b(nbtScheduledTick.func_74779_i("i"));
            } else {
               block = Block.func_149729_e(nbtScheduledTick.func_74762_e("i"));
            }

            if (block != null) {
               world.func_180497_b(
                  new BlockPos(nbtScheduledTick.func_74762_e("x"), nbtScheduledTick.func_74762_e("y"), nbtScheduledTick.func_74762_e("z")),
                  block,
                  nbtScheduledTick.func_74762_e("t"),
                  nbtScheduledTick.func_74762_e("p")
               );
            }
         }
      }
   }

   private static void readLightingInfo(Cube cube, NBTTagCompound nbt, World world) {
      NBTTagCompound lightingInfo = nbt.func_74775_l("LightingInfo");
      int[] lastHeightMap = lightingInfo.func_74759_k("LastHeightMap");
      int[] currentHeightMap = cube.getColumn().func_177445_q();
      byte edgeNeedSkyLightUpdate = 63;
      if (lightingInfo.func_74764_b("EdgeNeedSkyLightUpdate")) {
         edgeNeedSkyLightUpdate = lightingInfo.func_74771_c("EdgeNeedSkyLightUpdate");
      }

      LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = cube.getCubeLightUpdateInfo();
      if (cubeLightUpdateInfo != null) {
         for (int i = 0; i < EnumFacing.field_82609_l.length; i++) {
            if ((edgeNeedSkyLightUpdate >>> i & 1) != 0) {
               cubeLightUpdateInfo.markEdgeNeedSkyLightUpdate(EnumFacing.field_82609_l[i]);
            }
         }
      }

      int minBlockY = Coords.cubeToMinBlock(cube.getY());
      int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
      LightingManager lightManager = ((ICubicWorldInternal)cube.getWorld()).getLightingManager();

      for (int ix = 0; ix < currentHeightMap.length; ix++) {
         int currentY = currentHeightMap[ix];
         int lastY = lastHeightMap[ix];
         int minUpdateY = Math.min(currentY, lastY);
         int maxUpdateY = Math.max(currentY, lastY);
         boolean needLightUpdate = minUpdateY != maxUpdateY && maxUpdateY >= minBlockY && minUpdateY <= maxBlockY;
         if (needLightUpdate) {
            if (minUpdateY < minBlockY) {
               minUpdateY = minBlockY;
            }

            if (maxUpdateY > maxBlockY) {
               maxUpdateY = maxBlockY;
            }

            assert minUpdateY <= maxUpdateY : "minUpdateY > maxUpdateY: " + minUpdateY + ">" + maxUpdateY;

            int localX = ix & 15;
            int localZ = ix >> 4;
            lightManager.markCubeBlockColumnForUpdate(cube, Coords.localToBlock(cube.getX(), localX), Coords.localToBlock(cube.getZ(), localZ));
         }
      }
   }

   private static void readBiomes(Cube cube, NBTTagCompound nbt) {
      if (nbt.func_74764_b("Biomes")) {
         cube.setBiomeArray(nbt.func_74770_j("Biomes"));
      }
   }
}
