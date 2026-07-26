package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RenderCubeCache extends ChunkCache {
   protected int cubeY;
   @Nonnull
   private final ExtendedBlockStorage[][][] cubeArrays;
   @Nonnull
   private final Map<BlockPos, TileEntity>[][][] tileEntities;
   @Nonnull
   private final World world;

   public RenderCubeCache(World world, BlockPos from, BlockPos to, int subtract) {
      super(world, from, to, subtract);
      this.world = world;
      this.cubeY = Coords.blockToCube(from.func_177956_o() - subtract);
      int cubeXEnd = Coords.blockToCube(to.func_177958_n() + subtract);
      int cubeYEnd = Coords.blockToCube(to.func_177956_o() + subtract);
      int cubeZEnd = Coords.blockToCube(to.func_177952_p() + subtract);
      this.cubeArrays = new ExtendedBlockStorage[cubeXEnd - this.field_72818_a + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.field_72816_b + 1];
      Map<BlockPos, TileEntity>[][][] tileEntities = new Map[cubeXEnd - this.field_72818_a + 1][cubeYEnd - this.cubeY + 1][cubeZEnd - this.field_72816_b + 1];
      this.tileEntities = tileEntities;
      ExtendedBlockStorage nullStorage = new ExtendedBlockStorage(0, true);

      for (int currentCubeX = this.field_72818_a; currentCubeX <= cubeXEnd; currentCubeX++) {
         for (int currentCubeY = this.cubeY; currentCubeY <= cubeYEnd; currentCubeY++) {
            for (int currentCubeZ = this.field_72816_b; currentCubeZ <= cubeZEnd; currentCubeZ++) {
               Cube cube = ((ICubicWorldInternal)world).getCubeFromCubeCoords(currentCubeX, currentCubeY, currentCubeZ);
               ExtendedBlockStorage ebs = cube.getStorage();
               Map<BlockPos, TileEntity> teMap = cube.getTileEntityMap();
               if (ebs == null) {
                  ebs = nullStorage;
               }

               this.cubeArrays[currentCubeX - this.field_72818_a][currentCubeY - this.cubeY][currentCubeZ - this.field_72816_b] = ebs;
               tileEntities[currentCubeX - this.field_72818_a][currentCubeY - this.cubeY][currentCubeZ - this.field_72816_b] = teMap;
            }
         }
      }
   }

   public int func_175626_b(BlockPos pos, int lightValue) {
      int blockLight = this.getLightForExt(EnumSkyBlock.SKY, pos);
      int skyLight = this.getLightForExt(EnumSkyBlock.BLOCK, pos);
      if (skyLight < lightValue) {
         skyLight = lightValue;
      }

      return blockLight << 20 | skyLight << 4;
   }

   @Nullable
   public TileEntity func_175625_s(BlockPos pos) {
      int arrayX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
      int arrayY = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
      int arrayZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
      return arrayX >= 0
            && arrayX < this.cubeArrays.length
            && arrayY >= 0
            && arrayY < this.cubeArrays[arrayX].length
            && arrayZ >= 0
            && arrayZ < this.cubeArrays[arrayX][arrayY].length
         ? this.tileEntities[arrayX][arrayY][arrayZ].get(pos)
         : null;
   }

   public IBlockState func_180495_p(BlockPos pos) {
      if (this.world.func_189509_E(pos)) {
         return Blocks.field_150350_a.func_176223_P();
      } else {
         int arrayX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
         int arrayY = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
         int arrayZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
         return arrayX >= 0
               && arrayX < this.cubeArrays.length
               && arrayY >= 0
               && arrayY < this.cubeArrays[arrayX].length
               && arrayZ >= 0
               && arrayZ < this.cubeArrays[arrayX][arrayY].length
            ? this.cubeArrays[arrayX][arrayY][arrayZ]
               .func_177485_a(Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p()))
            : Blocks.field_150350_a.func_176223_P();
      }
   }

   private int getLightForExt(EnumSkyBlock type, BlockPos pos) {
      if (type == EnumSkyBlock.SKY && !this.world.field_73011_w.func_191066_m()) {
         return 0;
      } else if (this.world.func_189509_E(pos)) {
         return type.field_77198_c;
      } else if (this.func_180495_p(pos).func_185916_f()) {
         int max = 0;

         for (EnumFacing enumfacing : EnumFacing.values()) {
            int current = this.func_175628_b(type, pos.func_177972_a(enumfacing));
            if (current > max) {
               max = current;
            }

            if (max >= 15) {
               return max;
            }
         }

         return max;
      } else {
         int arrayX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
         int arrayY = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
         int arrayZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
         if (arrayX >= 0
            && arrayX < this.cubeArrays.length
            && arrayY >= 0
            && arrayY < this.cubeArrays[arrayX].length
            && arrayZ >= 0
            && arrayZ < this.cubeArrays[arrayX][arrayY].length) {
            ExtendedBlockStorage cube = this.cubeArrays[arrayX][arrayY][arrayZ];
            return this.getRawLight(cube, type, pos);
         } else {
            return type.field_77198_c;
         }
      }
   }

   public int func_175628_b(EnumSkyBlock type, BlockPos pos) {
      if (this.world.func_189509_E(pos)) {
         return type.field_77198_c;
      } else {
         int arrayX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
         int arrayY = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
         int arrayZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
         if (arrayX >= 0
            && arrayX < this.cubeArrays.length
            && arrayY >= 0
            && arrayY < this.cubeArrays[arrayX].length
            && arrayZ >= 0
            && arrayZ < this.cubeArrays[arrayX][arrayY].length) {
            ExtendedBlockStorage cube = this.cubeArrays[arrayX][arrayY][arrayZ];
            return this.getRawLight(cube, type, pos);
         } else {
            return type.field_77198_c;
         }
      }
   }

   private int getRawLight(ExtendedBlockStorage ebs, EnumSkyBlock type, BlockPos pos) {
      return type == EnumSkyBlock.BLOCK
         ? ebs.func_76674_d(Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p()))
         : ebs.func_76670_c(Coords.blockToLocal(pos.func_177958_n()), Coords.blockToLocal(pos.func_177956_o()), Coords.blockToLocal(pos.func_177952_p()));
   }

   public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
      if (this.world.func_189509_E(pos)) {
         return defaultValue;
      } else {
         int arrayX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
         int arrayY = Coords.blockToCube(pos.func_177956_o()) - this.cubeY;
         int arrayZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
         if (arrayX >= 0
            && arrayX < this.cubeArrays.length
            && arrayY >= 0
            && arrayY < this.cubeArrays[arrayX].length
            && arrayZ >= 0
            && arrayZ < this.cubeArrays[arrayX][arrayY].length) {
            IBlockState state = this.func_180495_p(pos);
            return state.func_177230_c().isSideSolid(state, this, pos, side);
         } else {
            return defaultValue;
         }
      }
   }
}
