package io.github.opencubicchunks.cubicchunks.core.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;

public class PacketUtils {
    private PacketUtils() {
    }

    public static void writeVarInt(FriendlyByteBuf buf, int value) {
        VarInt.write(buf, value);
    }

    public static int readVarInt(FriendlyByteBuf buf) {
        return VarInt.read(buf);
    }

    public static String toHex(FriendlyByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        int read = buf.readerIndex();
        sb.append('[');
        for (int i = read; i < Math.min(buf.writerIndex(), read + 32); i++) {
            sb.append(String.format("%02x ", buf.getUnsignedByte(i)));
        }
        sb.append(']');
        return sb.toString();
    }
}
