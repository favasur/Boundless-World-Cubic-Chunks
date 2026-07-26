package io.github.opencubicchunks.cubicchunks.api.util;

import net.minecraft.core.BlockPos;

/**
 * Converts between local cube coordinates and packed integer addresses.
 * Ported from 1.12.2 Cubic Chunks.
 */
// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.AddressTools
public final class AddressTools {
    private AddressTools() {
        throw new AssertionError("Utility class");
    }

    public static int getLocalAddress(int localX, int localY, int localZ) {
        return Bits.packUnsignedToInt(localX, 4, 0)
             | Bits.packUnsignedToInt(localZ, 4, 4)
             | Bits.packUnsignedToInt(localY, 4, 8);
    }

    public static int getLocalAddress(int localX, int localZ) {
        return Bits.packUnsignedToInt(localX, 4, 0)
             | Bits.packUnsignedToInt(localZ, 4, 4);
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
        return getLocalAddress(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getBiomeAddress(int biomeX, int biomeZ) {
        return biomeX << 3 | biomeZ;
    }
}
