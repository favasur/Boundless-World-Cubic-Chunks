package io.github.opencubicchunks.cubicchunks.api.util;

/**
 * Bit-packing helpers used by address tools and cube storage.
 * Ported from 1.12.2 Cubic Chunks.
 */
// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.util.Bits
public final class Bits {
    private Bits() {
        throw new AssertionError("Utility class");
    }

    public static long packUnsignedToLong(int unsigned, int size, int offset) {
        return packSignedToLong(unsigned, size, offset);
    }

    public static long packSignedToLong(int signed, int size, int offset) {
        long result = (long) (signed & getMask(size));
        return result << offset;
    }

    public static int packUnsignedToInt(int unsigned, int size, int offset) {
        return packSignedToInt(unsigned, size, offset);
    }

    public static int packSignedToInt(int signed, int size, int offset) {
        int result = signed & getMask(size);
        return result << offset;
    }

    public static int unpackUnsigned(long packed, int size, int offset) {
        packed >>= offset;
        return (int) packed & getMask(size);
    }

    public static int unpackSigned(long packed, int size, int offset) {
        int complementOffset = 64 - offset - size;
        packed = packed << complementOffset >> complementOffset;
        packed >>= offset;
        return (int) packed;
    }

    public static int unpackUnsigned(int packed, int size, int offset) {
        packed >>= offset;
        return packed & getMask(size);
    }

    public static int unpackSigned(int packed, int size, int offset) {
        int complementOffset = 64 - offset - size;
        packed = packed << complementOffset >> complementOffset;
        return packed >> offset;
    }

    public static int getMask(int size) {
        assert size > 0 && size < 32;
        return -1 >>> (32 - size);
    }

    public static int getMinSigned(int size) {
        return -(1 << (size - 1));
    }

    public static int getMaxSigned(int size) {
        return (1 << (size - 1)) - 1;
    }

    public static int getMaxUnsigned(int size) {
        return (1 << size) - 1;
    }
}
