package io.github.opencubicchunks.cubicchunks.api.util;

import java.util.Random;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubePos {
   public static final CubePos ZERO = new CubePos(0, 0, 0);
   private static final int Y_BITS = 20;
   private static final int X_BITS = 22;
   private static final int Z_BITS = 22;
   private static final int Z_BIT_OFFSET = 0;
   private static final int X_BIT_OFFSET = 22;
   private static final int Y_BIT_OFFSET = 44;
   private final int cubeX;
   private final int cubeY;
   private final int cubeZ;

   public CubePos(int cubeX, int cubeY, int cubeZ) {
      this.cubeX = cubeX;
      this.cubeY = cubeY;
      this.cubeZ = cubeZ;
   }

   public CubePos(XYZAddressable addressable) {
      this.cubeX = addressable.getX();
      this.cubeY = addressable.getY();
      this.cubeZ = addressable.getZ();
   }

   public int getX() {
      return this.cubeX;
   }

   public int getY() {
      return this.cubeY;
   }

   public int getZ() {
      return this.cubeZ;
   }

   @Override
   public String toString() {
      return String.format("CubePos(%d, %d, %d)", this.cubeX, this.cubeY, this.cubeZ);
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (obj == null) {
         return false;
      } else if (obj == this) {
         return true;
      } else if (!(obj instanceof CubePos)) {
         return false;
      } else {
         CubePos otherCoords = (CubePos)obj;
         return otherCoords.cubeX == this.cubeX && otherCoords.cubeY == this.cubeY && otherCoords.cubeZ == this.cubeZ;
      }
   }

   @Override
   public int hashCode() {
      return Long.hashCode(Bits.packSignedToLong(this.cubeX, 20, 44) | Bits.packSignedToLong(this.cubeY, 22, 22) | Bits.packSignedToLong(this.cubeZ, 22, 0));
   }

   public int getXCenter() {
      return this.cubeX * 16 + 8;
   }

   public int getYCenter() {
      return this.cubeY * 16 + 8;
   }

   public int getZCenter() {
      return this.cubeZ * 16 + 8;
   }

   public int getMinBlockX() {
      return Coords.cubeToMinBlock(this.cubeX);
   }

   public int getMinBlockY() {
      return Coords.cubeToMinBlock(this.cubeY);
   }

   public int getMinBlockZ() {
      return Coords.cubeToMinBlock(this.cubeZ);
   }

   public int getMaxBlockX() {
      return Coords.cubeToMaxBlock(this.cubeX);
   }

   public int getMaxBlockY() {
      return Coords.cubeToMaxBlock(this.cubeY);
   }

   public int getMaxBlockZ() {
      return Coords.cubeToMaxBlock(this.cubeZ);
   }

   public BlockPos getCenterBlockPos() {
      return new BlockPos(this.getXCenter(), this.getYCenter(), this.getZCenter());
   }

   public BlockPos getMinBlockPos() {
      return new BlockPos(this.getMinBlockX(), this.getMinBlockY(), this.getMinBlockZ());
   }

   public BlockPos getMaxBlockPos() {
      return new BlockPos(this.getMaxBlockX(), this.getMaxBlockY(), this.getMaxBlockZ());
   }

   public BlockPos localToBlock(int localX, int localY, int localZ) {
      return new BlockPos(this.getMinBlockX() + localX, this.getMinBlockY() + localY, this.getMinBlockZ() + localZ);
   }

   public CubePos sub(int dx, int dy, int dz) {
      return this.add(-dx, -dy, -dz);
   }

   public CubePos add(int dx, int dy, int dz) {
      return new CubePos(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
   }

   public ChunkPos chunkPos() {
      return new ChunkPos(this.getX(), this.getZ());
   }

   public int distSquared(CubePos coords) {
      int dx = coords.cubeX - this.cubeX;
      int dy = coords.cubeY - this.cubeY;
      int dz = coords.cubeZ - this.cubeZ;
      return dx * dx + dy * dy + dz * dz;
   }

   public void forEachWithinRange(int range, Consumer<CubePos> action) {
      for (int x = this.cubeX - range; x < this.cubeX + range; x++) {
         for (int y = this.cubeY - range; y < this.cubeY + range; y++) {
            for (int z = this.cubeZ - range; z < this.cubeZ + range; z++) {
               action.accept(new CubePos(x, y, z));
            }
         }
      }
   }

   public BlockPos randomPopulationPos(Random rand) {
      return new BlockPos(rand.nextInt(16) + this.getXCenter(), rand.nextInt(16) + this.getYCenter(), rand.nextInt(16) + this.getZCenter());
   }

   public CubePos above() {
      return this.add(0, 1, 0);
   }

   public CubePos below() {
      return this.add(0, -1, 0);
   }

   public static CubePos fromBlockCoords(int blockX, int blockY, int blockZ) {
      return new CubePos(Coords.blockToCube(blockX), Coords.blockToCube(blockY), Coords.blockToCube(blockZ));
   }

   public static CubePos fromEntityCoords(double blockX, double blockY, double blockZ) {
      return new CubePos(Coords.blockToCube(blockX), Coords.blockToCube(blockY), Coords.blockToCube(blockZ));
   }

   public static CubePos fromEntity(Entity entity) {
      return new CubePos(Coords.getCubeXForEntity(entity), Coords.getCubeYForEntity(entity), Coords.getCubeZForEntity(entity));
   }

   public static CubePos fromBlockCoords(BlockPos pos) {
      return fromBlockCoords(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
   }

   public boolean containsBlock(BlockPos pos) {
      return this.cubeX == Coords.blockToCube(pos.func_177958_n())
         && this.cubeY == Coords.blockToCube(pos.func_177956_o())
         && this.cubeZ == Coords.blockToCube(pos.func_177952_p());
   }
}
