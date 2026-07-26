package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketCubes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// @Original: N/A — modern wrapper for the in-batch multi-cube packet.
public record MultiCubeDataPayload(PacketCubes packet) implements CustomPacketPayload {
    public static final Type<MultiCubeDataPayload> TYPE = new Type<>(PacketCubes.ID);
    public static final StreamCodec<FriendlyByteBuf, MultiCubeDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PacketCubes.write(payload.packet(), buf),
            buf -> new MultiCubeDataPayload(PacketCubes.from(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
