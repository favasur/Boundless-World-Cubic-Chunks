package io.github.opencubicchunks.cubicchunks.core.network.payload;

import io.github.opencubicchunks.cubicchunks.core.network.packet.PacketColumn;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// @Original: N/A — 1.21 wrapper for the cross-loader packet contract.
// 1.21: ChunkPos byte layout is x(4) | z(4) | byte-array length + bytes.
public record ColumnDataPayload(PacketColumn packet) implements CustomPacketPayload {
    public static final Type<ColumnDataPayload> TYPE = new Type<>(PacketColumn.ID);
    public static final StreamCodec<FriendlyByteBuf, ColumnDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PacketColumn.write(payload.packet(), buf),
            buf -> new ColumnDataPayload(PacketColumn.from(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
