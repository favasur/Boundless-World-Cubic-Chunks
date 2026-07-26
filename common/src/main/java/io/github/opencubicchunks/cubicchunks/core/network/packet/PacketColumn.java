package io.github.opencubicchunks.cubicchunks.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketColumn
// 1.21: cube column metadata (heightmap patches). Vanilla SPacketChunkData is unchanged;
// this packet only carries post-load patches.
public class PacketColumn implements IPacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "column_data");

    private ChunkPos chunkPos = new ChunkPos(0, 0);
    private byte[] data = new byte[0];

    public PacketColumn() {
    }

    public PacketColumn(LevelChunk column, byte[] encoded) {
        this.chunkPos = column.getPos();
        this.data = encoded != null ? encoded : new byte[0];
    }

    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
        int size = buf.readInt();
        this.data = new byte[size];
        buf.readBytes(this.data);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.chunkPos.x);
        buf.writeInt(this.chunkPos.z);
        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);
    }

    public ChunkPos getChunkPos() {
        return this.chunkPos;
    }

    public byte[] getData() {
        return this.data;
    }

    public static PacketColumn from(FriendlyByteBuf buf) {
        PacketColumn p = new PacketColumn();
        p.readFromBuf(buf);
        return p;
    }

    public static void write(PacketColumn p, FriendlyByteBuf buf) {
        p.write((FriendlyByteBuf) buf);
    }
}
