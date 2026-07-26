package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubeSkyLightUpdates;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// @Original: N/A — wrapper for the existing 1.21 packet format.
public record CubeSkyLightPayload(PacketCubeSkyLightUpdates packet) implements CustomPacketPayload {
    public static final Type<CubeSkyLightPayload> TYPE = new Type<>(PacketCubeSkyLightUpdates.ID);
    public static final StreamCodec<FriendlyByteBuf, CubeSkyLightPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PacketCubeSkyLightUpdates.write(payload.packet(), buf),
            buf -> new CubeSkyLightPayload(PacketCubeSkyLightUpdates.from(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
