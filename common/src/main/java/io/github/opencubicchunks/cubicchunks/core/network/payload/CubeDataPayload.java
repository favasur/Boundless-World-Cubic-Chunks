package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundCubeDataPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CubeDataPayload(ClientboundCubeDataPacket packet) implements CustomPacketPayload {
    public static final Type<CubeDataPayload> TYPE = new Type<>(ClientboundCubeDataPacket.ID);
    public static final StreamCodec<FriendlyByteBuf, CubeDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.packet().write(buf),
            buf -> new CubeDataPayload(ClientboundCubeDataPacket.read(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
