package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.lighting.ILightBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FastCubeBlockAccess implements ILightBlockAccess {
   @Nonnull
   private final ExtendedBlockStorage[][][] cache;
   @Nonnull
   private final Cube[][][] cubes;
   @Nonnull
   private final Chunk[][] columns;
   private final int originX;
   private final int originY;
   private final int originZ;
   private final int dx;
   private final int dy;
   private final int dz;
   @Nonnull
   private final World world;

   public FastCubeBlockAccess(ICubeProviderInternal cache, ICube cube, int radius) {
      this(cube.getWorld(), cache, cube.getCoords().sub(radius, radius, radius), cube.getCoords().add(radius, radius, radius));
   }

   private FastCubeBlockAccess(World world, ICubeProviderInternal prov, CubePos start, CubePos end) {
      this.dx = Math.abs(end.getX() - start.getX()) + 1;
      this.dy = Math.abs(end.getY() - start.getY()) + 1;
      this.dz = Math.abs(end.getZ() - start.getZ()) + 1;
      this.world = world;
      this.cache = new ExtendedBlockStorage[this.dx][this.dy][this.dz];
      this.cubes = new Cube[this.dx][this.dy][this.dz];
      this.columns = new Chunk[this.dx][this.dz];
      this.originX = Math.min(start.getX(), end.getX());
      this.originY = Math.min(start.getY(), end.getY());
      this.originZ = Math.min(start.getZ(), end.getZ());

      for (int relativeCubeX = 0; relativeCubeX < this.dx; relativeCubeX++) {
         for (int relativeCubeZ = 0; relativeCubeZ < this.dz; relativeCubeZ++) {
            this.columns[relativeCubeX][relativeCubeZ] = prov.getLoadedColumn(this.originX + relativeCubeX, this.originZ + relativeCubeZ);

            for (int relativeCubeY = 0; relativeCubeY < this.dy; relativeCubeY++) {
               Cube cube = prov.getLoadedCube(this.originX + relativeCubeX, this.originY + relativeCubeY, this.originZ + relativeCubeZ);
               if (cube != null) {
                  ExtendedBlockStorage storage = cube.getStorage();
                  this.cache[relativeCubeX][relativeCubeY][relativeCubeZ] = storage;
                  this.cubes[relativeCubeX][relativeCubeY][relativeCubeZ] = cube;
                  cube.markDirty();
               }
            }
         }
      }
   }

   @Nullable
   private ExtendedBlockStorage getStorage(int blockX, int blockY, int blockZ) {
      int cubeX = Coords.blockToCube(blockX);
      int cubeY = Coords.blockToCube(blockY);
      int cubeZ = Coords.blockToCube(blockZ);
      if (cubeX >= this.originX && cubeY >= this.originY && cubeZ >= this.originZ) {
         cubeX -= this.originX;
         cubeY -= this.originY;
         cubeZ -= this.originZ;
         return cubeX < this.dx && cubeY < this.dy && cubeZ < this.dz ? this.cache[cubeX][cubeY][cubeZ] : null;
      } else {
         return null;
      }
   }

   private void setStorage(int blockX, int blockY, int blockZ, @Nullable ExtendedBlockStorage ebs) {
      int cubeX = Coords.blockToCube(blockX);
      int cubeY = Coords.blockToCube(blockY);
      int cubeZ = Coords.blockToCube(blockZ);
      this.cache[cubeX - this.originX][cubeY - this.originY][cubeZ - this.originZ] = ebs;
   }

   @Nullable
   private Cube getCube(int blockX, int blockY, int blockZ) {
      int cubeX = Coords.blockToCube(blockX);
      int cubeY = Coords.blockToCube(blockY);
      int cubeZ = Coords.blockToCube(blockZ);
      if (cubeX >= this.originX && cubeY >= this.originY && cubeZ >= this.originZ) {
         cubeX -= this.originX;
         cubeY -= this.originY;
         cubeZ -= this.originZ;
         return cubeX < this.dx && cubeY < this.dy && cubeZ < this.dz ? this.cubes[cubeX][cubeY][cubeZ] : null;
      } else {
         return null;
      }
   }

   private IBlockState getBlockState(BlockPos pos) {
      return this.getBlockState(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
   }

   private IBlockState getBlockState(int blockX, int blockY, int blockZ) {
      ExtendedBlockStorage ebs = this.getStorage(blockX, blockY, blockZ);
      return ebs != null
         ? ebs.func_177485_a(Coords.blockToLocal(blockX), Coords.blockToLocal(blockY), Coords.blockToLocal(blockZ))
         : Blocks.field_150350_a.func_176223_P();
   }

   @Override
   public int getBlockLightOpacity(BlockPos pos) {
      return this.getBlockState(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()).getLightOpacity(this.world, pos);
   }

   @Override
   public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
      ExtendedBlockStorage ebs = this.getStorage(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
      if (ebs != null) {
         int localX = Coords.blockToLocal(pos.func_177958_n());
         int localY = Coords.blockToLocal(pos.func_177956_o());
         int localZ = Coords.blockToLocal(pos.func_177952_p());
         return lightType == EnumSkyBlock.SKY ? ebs.func_76670_c(localX, localY, localZ) : ebs.func_76674_d(localX, localY, localZ);
      } else {
         return 0;
      }
   }

   @Override
   public boolean setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
      ExtendedBlockStorage ebs = this.getStorage(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
      if (ebs != null) {
         int localX = Coords.blockToLocal(pos.func_177958_n());
         int localY = Coords.blockToLocal(pos.func_177956_o());
         int localZ = Coords.blockToLocal(pos.func_177952_p());
         if (lightType == EnumSkyBlock.SKY) {
            ebs.func_76657_c(localX, localY, localZ, val);
         } else {
            ebs.func_76677_d(localX, localY, localZ, val);
         }

         return true;
      } else {
         Cube cube = this.getCube(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
         if (cube != null) {
            cube.setLightFor(lightType, pos, val);
            this.setStorage(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), cube.getStorage());
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean canSeeSky(BlockPos pos) {
      int blockX = pos.func_177958_n();
      int blockY = pos.func_177956_o();
      int blockZ = pos.func_177952_p();
      int cubeX = Coords.blockToCube(blockX);
      int cubeZ = Coords.blockToCube(blockZ);
      if (cubeX >= this.originX && cubeZ >= this.originZ) {
         cubeX -= this.originX;
         cubeZ -= this.originZ;
         if (cubeX < this.dx && cubeZ < this.dz) {
            Chunk column = this.columns[cubeX][cubeZ];
            if (column == null) {
               return false;
            } else {
               int height = ((IColumnInternal)column).getHeightWithStaging(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
               return height <= blockY;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
      switch (type) {
         case BLOCK:
            return this.getBlockState(pos).getLightValue(this.world, pos);
         case SKY:
            return this.canSeeSky(pos) ? 15 : 0;
         default:
            throw new AssertionError();
      }
   }

   public static ILightBlockAccess forBlockRegion(ICubeProviderInternal prov, BlockPos startPos, BlockPos endPos) {
      BlockPos midPos = Coords.midPos(startPos, endPos);
      Cube center = prov.getCube(CubePos.fromBlockCoords(midPos));
      return new FastCubeBlockAccess(center.getWorld(), prov, CubePos.fromBlockCoords(startPos), CubePos.fromBlockCoords(endPos));
   }

   @Override
   public void markEdgeNeedLightUpdate(BlockPos pos, EnumSkyBlock type) {
      if (type != EnumSkyBlock.BLOCK) {
         int x = pos.func_177958_n();
         int y = pos.func_177956_o();
         int z = pos.func_177952_p();
         Cube cube = this.getCube(x, y, z);
         if (cube != null) {
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
}
