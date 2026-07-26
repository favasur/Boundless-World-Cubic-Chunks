package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AddressTools {
   public AddressTools() {
   }

   public static int getLocalAddress(int localX, int localY, int localZ) {
      return Bits.packUnsignedToInt(localX, 4, 0) | Bits.packUnsignedToInt(localZ, 4, 4) | Bits.packUnsignedToInt(localY, 4, 8);
   }

   public static int getLocalAddress(int localX, int localZ) {
      return Bits.packUnsignedToInt(localX, 4, 0) | Bits.packUnsignedToInt(localZ, 4, 4);
   }

   public static int getLocalX(int localAddress) {
      return Bits.unpackUnsigned(localAddress, 4, 0);
   }

   public static int getLocalY(int localAddress) {
      return Bits.unpackUnsigned(localAddress, 4, 8);
   }

   public static int getLocalZ(int localAddress) {
      return Bits.unpackUnsigned(localAddress, 4, 4);
   }

   public static int getLocalAddress(BlockPos pos) {
      return getLocalAddress(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
   }

   public static int getBiomeAddress(int biomeX, int biomeZ) {
      return biomeX << 3 | biomeZ;
   }
}
