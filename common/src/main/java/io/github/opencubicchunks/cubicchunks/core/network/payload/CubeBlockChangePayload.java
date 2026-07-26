package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubeBlockChange;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// @Original: N/A — wrapper for the existing 1.21 packet format.
public record CubeBlockChangePayload(PacketCubeBlockChange packet) implements CustomPacketPayload {
    public static final Type<CubeBlockChangePayload> TYPE = new Type<>(PacketCubeBlockChange.ID);
    public static final StreamCodec<FriendlyByteBuf, CubeBlockChangePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PacketCubeBlockChange.write(payload.packet(), buf),
            buf -> new CubeBlockChangePayload(PacketCubeBlockChange.from(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
