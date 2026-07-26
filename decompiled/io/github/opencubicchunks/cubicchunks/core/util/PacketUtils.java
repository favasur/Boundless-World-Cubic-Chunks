package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;

public class PacketUtils {
   private static final int MASK_6 = 63;
   private static final int MASK_7 = 127;

   public PacketUtils() {
   }

   public static void write(ByteBuf buf, BlockPos pos) {
      writeSignedVarInt(buf, pos.func_177958_n());
      writeSignedVarInt(buf, pos.func_177956_o());
      writeSignedVarInt(buf, pos.func_177952_p());
   }

   public static BlockPos readBlockPos(ByteBuf buf) {
      return new BlockPos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
   }

   public static void write(ByteBuf buf, CubePos pos) {
      writeSignedVarInt(buf, pos.getX());
      writeSignedVarInt(buf, pos.getY());
      writeSignedVarInt(buf, pos.getZ());
   }

   public static CubePos readCubePos(ByteBuf buf) {
      return new CubePos(readSignedVarInt(buf), readSignedVarInt(buf), readSignedVarInt(buf));
   }

   public static void writeSignedVarInt(ByteBuf buf, int i) {
      int signBit = i >>> 31 << 6;
      int val = i < 0 ? ~i : i;

      assert val >= 0;

      int var4;
      writeVarIntByte(buf, val & 63 | signBit, (var4 = val >> 6) > 0);

      while (var4 > 0) {
         writeVarIntByte(buf, var4 & 127, (var4 >>= 7) > 0);
      }
   }

   public static int readSignedVarInt(ByteBuf buf) {
      int val = 0;
      int b = buf.readUnsignedByte();
      boolean sign = (b >> 6 & 1) != 0;
      val |= b & 63;

      for (int shift = 6; (b & 128) != 0; shift += 7) {
         if (shift > 32) {
            throw new RuntimeException("VarInt too big");
         }

         b = buf.readUnsignedByte();
         val |= (b & 127) << shift;
      }

      return sign ? ~val : val;
   }

   private static void writeVarIntByte(ByteBuf buf, int i, boolean hasMore) {
      buf.writeByte(i | (hasMore ? 128 : 0));
   }
}
