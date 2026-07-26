package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SingleCubeNeighborLightAccess implements ILightBlockAccess, IBlockAccess {
   private final ExtendedBlockStorage[] storageArray = new ExtendedBlockStorage[6];
   private final Cube[] cubeArray = new Cube[6];
   private final Chunk[] columnArray = new Chunk[4];
   private final int cubeX;
   private final int cubeY;
   private final int cubeZ;
   private final Cube centerCube;
   private ExtendedBlockStorage centerStorage;
   private final Chunk centerColumn;
   private final WorldType worldType;

   public SingleCubeNeighborLightAccess(ICube cube) {
      int x = cube.getX();
      int y = cube.getY();
      int z = cube.getZ();

      for (EnumFacing value : EnumFacing.field_82609_l) {
         int offX = value.func_82601_c();
         int x1 = x + offX;
         int offY = value.func_96559_d();
         int y1 = y + offY;
         int offZ = value.func_82599_e();
         int z1 = z + offZ;
         int idx = getIndexByCube(offX, offY, offZ);
         ICube offsetCube = ((ICubicWorld)cube.getWorld()).getCubeCache().getLoadedCube(x1, y1, z1);
         if (offsetCube != null && offsetCube.isInitialLightingDone()) {
            this.cubeArray[idx] = (Cube)offsetCube;
            this.storageArray[idx] = offsetCube.getStorage();
            this.columnArray[getIndexByColumn(offX, offZ)] = offsetCube.getColumn();
         }
      }

      this.cubeX = x;
      this.cubeY = y;
      this.cubeZ = z;
      this.centerCube = (Cube)cube;
      this.centerColumn = cube.getColumn();
      this.centerStorage = cube.getStorage();
      this.worldType = cube.getWorld().func_72912_H().func_76067_t();
   }

   private static int getIndexByCube(int x, int y, int z) {
      return (x + y + z + 1 & 2) >> 1 | (x & 1) << 1 | (z & 1) << 2;
   }

   private static int getIndexByColumn(int x, int z) {
      return x & 1 | x + z + 1 & 2;
   }

   @Override
   public int getBlockLightOpacity(BlockPos pos) {
      int dx = Coords.blockToCube(pos.func_177958_n()) - this.cubeX;
      int dy = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int dz = Coords.blockToCube(pos.func_177952_p()) - this.cubeZ;
      ExtendedBlockStorage storage;
      if ((dx | dy | dz) == 0) {
         storage = this.centerStorage;
      } else {
         storage = this.storageArray[getIndexByCube(dx, dy, dz)];
      }

      return storage == null
         ? 0
         : storage.func_177485_a(Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p()))
            .getLightOpacity(this, pos);
   }

   @Override
   public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
      int dx = Coords.blockToCube(pos.func_177958_n()) - this.cubeX;
      int dy = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int dz = Coords.blockToCube(pos.func_177952_p()) - this.cubeZ;
      int x = Coords.blockToLocal(pos.func_177958_n());
      int y = Coords.blockToLocal(pos.func_177956_o());
      int z = Coords.blockToLocal(pos.func_177952_p());
      ExtendedBlockStorage storage;
      if ((dx | dy | dz) == 0) {
         storage = this.centerStorage;
      } else {
         int indexByCube = getIndexByCube(dx, dy, dz);
         storage = this.storageArray[indexByCube];
      }

      if (storage == null) {
         return 0;
      } else {
         return lightType == EnumSkyBlock.BLOCK ? storage.func_76674_d(x, y, z) : storage.func_76670_c(x, y, z);
      }
   }

   @Override
   public boolean setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
      int blockX = pos.func_177958_n();
      int x = Coords.blockToCube(blockX);
      int blockY = pos.func_177956_o();
      int y = Coords.blockToCube(blockY);
      int blockZ = pos.func_177952_p();
      int z = Coords.blockToCube(blockZ);
      if (this.cubeX == x && this.cubeY == y && this.cubeZ == z) {
         ExtendedBlockStorage storage = this.centerStorage;
         if (storage == null) {
            Cube cube = this.centerCube;
            storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()), cube.getWorld().field_73011_w.func_191066_m());
            cube.setStorage(storage);
            this.centerStorage = storage;
         }

         int xLocal = Coords.blockToLocal(pos.func_177958_n());
         int yLocal = Coords.blockToLocal(pos.func_177956_o());
         int zLocal = Coords.blockToLocal(pos.func_177952_p());
         if (lightType == EnumSkyBlock.SKY) {
            storage.func_76657_c(xLocal, yLocal, zLocal, val);
         } else {
            storage.func_76677_d(xLocal, yLocal, zLocal, val);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canSeeSky(BlockPos pos) {
      int blockX = pos.func_177958_n();
      int blockZ = pos.func_177952_p();
      int dx = Coords.blockToCube(blockX) - this.cubeX;
      int dz = Coords.blockToCube(blockZ) - this.cubeZ;
      Chunk chunk;
      if ((dx | dz) == 0) {
         chunk = this.centerColumn;
      } else {
         chunk = this.columnArray[getIndexByColumn(dx, dz)];
         if (chunk == null) {
            return false;
         }
      }

      int height = ((IColumnInternal)chunk).getHeightWithStaging(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
      return pos.func_177956_o() >= height;
   }

   @Override
   public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
      if (type == EnumSkyBlock.BLOCK) {
         return this.func_180495_p(pos).getLightValue(this, pos);
      } else {
         return this.canSeeSky(pos) ? 15 : 0;
      }
   }

   @Override
   public void markEdgeNeedLightUpdate(BlockPos pos, EnumSkyBlock type) {
      if (type != EnumSkyBlock.BLOCK) {
         int x = pos.func_177958_n();
         int y = pos.func_177956_o();
         int z = pos.func_177952_p();
         if (Coords.blockToCube(x) == this.cubeX && Coords.blockToCube(y) == this.cubeY && Coords.blockToCube(z) == this.cubeZ) {
            Cube cube = this.centerCube;
            int localX = Coords.blockToLocal(x);
            int localY = Coords.blockToLocal(y);
            int localZ = Coords.blockToLocal(z);
            if (localX == 0) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.WEST);
            } else if (localX == 15) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.EAST);
            }

            if (localY == 0) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.DOWN);
            } else if (localY == 15) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.UP);
            }

            if (localZ == 0) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.NORTH);
            } else if (localZ == 15) {
               cube.markEdgeNeedSkyLightUpdate(EnumFacing.SOUTH);
            }
         }
      }
   }

   @Override
   public boolean hasNeighborsAccessible(BlockPos pos) {
      return this.cubeX == Coords.blockToCube(pos.func_177958_n())
         && this.cubeY == Coords.blockToCube(pos.func_177956_o())
         && this.cubeZ == Coords.blockToCube(pos.func_177952_p());
   }

   @Nullable
   public TileEntity func_175625_s(BlockPos pos) {
      int dx = Coords.blockToCube(pos.func_177958_n()) - this.cubeX;
      int dy = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int dz = Coords.blockToCube(pos.func_177952_p()) - this.cubeZ;
      Cube cube;
      if ((dx | dy | dz) == 0) {
         cube = this.centerCube;
      } else {
         cube = this.cubeArray[getIndexByCube(dx, dy, dz)];
         if (cube == null) {
            return null;
         }
      }

      return cube.getTileEntityMap().get(pos);
   }

   public int func_175626_b(BlockPos pos, int lightValue) {
      int skyLight = this.getLightFor(EnumSkyBlock.SKY, pos);
      int blockLight = this.getLightFor(EnumSkyBlock.BLOCK, pos);
      if (blockLight < lightValue) {
         blockLight = lightValue;
      }

      return skyLight << 20 | blockLight << 4;
   }

   public IBlockState func_180495_p(BlockPos pos) {
      int dx = Coords.blockToCube(pos.func_177958_n()) - this.cubeX;
      int dy = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int dz = Coords.blockToCube(pos.func_177952_p()) - this.cubeZ;
      ExtendedBlockStorage storage;
      if ((dx | dy | dz) == 0) {
         storage = this.centerStorage;
      } else {
         storage = this.storageArray[getIndexByCube(dx, dy, dz)];
      }

      return storage == null
         ? Blocks.field_150350_a.func_176223_P()
         : storage.func_177485_a(Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p()));
   }

   public boolean func_175623_d(BlockPos pos) {
      return this.func_180495_p(pos).func_177230_c() == Blocks.field_150350_a;
   }

   public Biome func_180494_b(BlockPos pos) {
      int blockX = pos.func_177958_n();
      int blockZ = pos.func_177952_p();
      int dx = Coords.blockToCube(blockX) - this.cubeX;
      int dz = Coords.blockToCube(blockZ) - this.cubeZ;
      Chunk chunk;
      if ((dx | dz) == 0) {
         chunk = this.centerColumn;
      } else {
         chunk = this.columnArray[getIndexByColumn(dx, dz)];
         if (chunk == null) {
            return Biomes.field_76772_c;
         }
      }

      return chunk.func_177411_a(pos, chunk.func_177412_p().func_72959_q());
   }

   public int func_175627_a(BlockPos pos, EnumFacing direction) {
      return this.func_180495_p(pos).func_185893_b(this, pos, direction);
   }

   public WorldType func_175624_G() {
      return this.worldType;
   }

   public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
      int dx = Coords.blockToCube(pos.func_177958_n()) - this.cubeX;
      int dy = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int dz = Coords.blockToCube(pos.func_177952_p()) - this.cubeZ;
      ExtendedBlockStorage storage;
      if ((dx | dy | dz) == 0) {
         storage = this.centerStorage;
      } else {
         storage = this.storageArray[getIndexByCube(dx, dy, dz)];
      }

      if (storage == null) {
         return _default;
      } else {
         IBlockState state = storage.func_177485_a(
            Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p())
         );
         return state.func_177230_c().isSideSolid(state, this, pos, side);
      }
   }
}
