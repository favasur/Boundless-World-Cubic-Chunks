package io.github.opencubicchunks.cubicchunks.core.world;

import com.google.common.base.Throwables;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClientHeightMap implements IHeightMap {
   private final Chunk column;
   private final IHeightMap.HeightMap hmap;
   private int heightMapLowest = -2147483616;

   public ClientHeightMap(Chunk column, int[] heightmap) {
      this.column = column;
      this.hmap = new IHeightMap.HeightMap(heightmap);
   }

   @Override
   public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
      this.writeNewTopBlockY(localX, blockY, localZ, opacity, this.getTopBlockY(localX, localZ));
   }

   private void writeNewTopBlockY(int localX, int changeY, int localZ, int newOpacity, int oldTopY) {
      if (this.addedTopBlock(changeY, newOpacity, oldTopY)) {
         this.setHeight(localX, localZ, changeY);
      } else if (this.changedTopToTransparent(changeY, newOpacity, oldTopY)) {
         assert newOpacity != 0 || oldTopY >= changeY : "Changed transparent block into transparent!";

         int newTop = oldTopY - 1;

         while (this.column.func_177437_b(new BlockPos(localX, newTop, localZ)) == 0 && newTop > oldTopY - 64) {
            newTop--;
         }

         this.setHeight(localX, localZ, newTop);
      }
   }

   private boolean changedTopToTransparent(int changeY, int newOpacity, int oldTopY) {
      return newOpacity == 0 && changeY == oldTopY;
   }

   private boolean addedTopBlock(int changeY, int newOpacity, int oldTopY) {
      return changeY > oldTopY && newOpacity != 0;
   }

   @Override
   public int getTopBlockY(int localX, int localZ) {
      return this.hmap.get(getIndex(localX, localZ));
   }

   @Override
   public int getLowestTopBlockY() {
      if (this.heightMapLowest == -2147483616) {
         this.heightMapLowest = Integer.MAX_VALUE;

         for (int i = 0; i < 256; i++) {
            if (this.hmap.get(i) < this.heightMapLowest) {
               this.heightMapLowest = this.hmap.get(i);
            }
         }
      }

      return this.heightMapLowest;
   }

   @Override
   public int getTopBlockYBelow(int localX, int localZ, int blockY) {
      throw new UnsupportedOperationException("Not implemented");
   }

   public void setHeight(int localX, int localZ, int height) {
      this.hmap.set(getIndex(localX, localZ), height);
   }

   public byte[] getData() {
      try {
         ByteArrayOutputStream buf = new ByteArrayOutputStream();
         DataOutputStream out = new DataOutputStream(buf);

         for (int i = 0; i < 256; i++) {
            out.writeInt(this.hmap.get(i));
         }

         out.close();
         return buf.toByteArray();
      } catch (IOException var4) {
         Throwables.throwIfUnchecked(var4);
         throw new AssertionError();
      }
   }

   public void setData(@Nonnull byte[] data) {
      try {
         ByteArrayInputStream buf = new ByteArrayInputStream(data);
         DataInputStream in = new DataInputStream(buf);

         for (int i = 0; i < 256; i++) {
            this.hmap.set(i, in.readInt());
         }

         in.close();
      } catch (IOException var5) {
         Throwables.throwIfUnchecked(var5);
         throw new AssertionError();
      }
   }

   private static int getIndex(int localX, int localZ) {
      return localZ << 4 | localX;
   }
}
