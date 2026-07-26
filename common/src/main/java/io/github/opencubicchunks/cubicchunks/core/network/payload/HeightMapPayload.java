package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketHeightMapUpdate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// @Original: N/A — wrapper for the existing 1.21 packet format.
public record HeightMapPayload(PacketHeightMapUpdate packet) implements CustomPacketPayload {
    public static final Type<HeightMapPayload> TYPE = new Type<>(PacketHeightMapUpdate.ID);
    public static final StreamCodec<FriendlyByteBuf, HeightMapPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PacketHeightMapUpdate.write(payload.packet(), buf),
            buf -> new HeightMapPayload(PacketHeightMapUpdate.from(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
