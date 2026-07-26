package io.github.opencubicchunks.cubicchunks.api.worldgen;

import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubePrimer {
   public static final IBlockState DEFAULT_STATE = Blocks.field_150350_a.func_176223_P();
   private final char[] data;
   private byte[] extData = null;
   private Biome[] biomes3d = null;

   public boolean hasBiomes() {
      return this.biomes3d != null;
   }

   public CubePrimer() {
      this(new char[4096]);
   }

   public static CubePrimer createFilled(IBlockState state) {
      int value = Block.field_176229_d.func_148747_b(state);
      char lsb = (char)value;
      char[] data = new char[4096];
      Arrays.fill(data, lsb);
      return new CubePrimer(data);
   }

   protected CubePrimer(char[] data) {
      this.data = data;
   }

   @Nullable
   public Biome getBiome(int localBiomeX, int localBiomeY, int localBiomeZ) {
      if (this.biomes3d == null) {
         return null;
      } else {
         int biomeX = localBiomeX * 2;
         int biomeZ = localBiomeZ * 2;
         return this.biomes3d[biomeX << 3 | biomeZ];
      }
   }

   public void setBiome(int localBiomeX, int localBiomeY, int localBiomeZ, Biome biome) {
      if (this.biomes3d == null) {
         this.biomes3d = new Biome[64];
      }

      int biomeX = localBiomeX * 2;
      int biomeZ = localBiomeZ * 2;

      for (int dx = 0; dx < 2; dx++) {
         for (int dz = 0; dz < 2; dz++) {
            this.biomes3d[biomeX + dx << 3 | biomeZ + dz] = biome;
         }
      }
   }

   public IBlockState getBlockState(int x, int y, int z) {
      int idx = getBlockIndex(x, y, z);
      int block = this.data[idx];
      if (this.extData != null) {
         block |= this.extData[idx] << 16;
      }

      IBlockState iblockstate = (IBlockState)Block.field_176229_d.func_148745_a(block);
      return iblockstate == null ? DEFAULT_STATE : iblockstate;
   }

   public void setBlockState(int x, int y, int z, @Nonnull IBlockState state) {
      int value = Block.field_176229_d.func_148747_b(state);
      char lsb = (char)value;
      int idx = getBlockIndex(x, y, z);
      this.data[idx] = lsb;
      if (value > 65535) {
         if (this.extData == null) {
            this.extData = new byte[4096];
         }

         this.extData[idx] = (byte)(value >>> 16);
      }
   }

   public void reset() {
      Arrays.fill(this.data, '\u0000');
      this.extData = null;
      this.biomes3d = null;
   }

   private static int getBlockIndex(int x, int y, int z) {
      return y << 8 | z << 4 | x;
   }
}
