package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UnloadCubePayload(ClientboundUnloadCubePacket packet) implements CustomPacketPayload {
    public static final Type<UnloadCubePayload> TYPE = new Type<>(ClientboundUnloadCubePacket.ID);
    public static final StreamCodec<FriendlyByteBuf, UnloadCubePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.packet().write(buf),
            buf -> new UnloadCubePayload(ClientboundUnloadCubePacket.read(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
