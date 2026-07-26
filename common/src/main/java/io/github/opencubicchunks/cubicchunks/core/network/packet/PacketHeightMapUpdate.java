package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketHeightMapUpdate
// 1.21: uses ICubicWorldInternal directly. Packed byte carries (x,z) and full int carries height.
public class PacketHeightMapUpdate implements IPacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "heightmap_update");

    private ChunkPos chunkPos = new ChunkPos(0, 0);
    private byte[] updates = new byte[0];
    private int[] heights = new int[0];

    public PacketHeightMapUpdate() {
    }

    public PacketHeightMapUpdate(ChunkPos chunkPos, byte[] updates, int[] heights) {
        this.chunkPos = chunkPos;
        this.updates = updates.clone();
        this.heights = heights.clone();
    }

    public ResourceLocation getId() {
        return ID;
    }

    public static PacketHeightMapUpdate from(FriendlyByteBuf buf) {
        PacketHeightMapUpdate p = new PacketHeightMapUpdate();
        p.readFromBuf(buf);
        return p;
    }

    public static void write(PacketHeightMapUpdate p, FriendlyByteBuf buf) {
        p.write((FriendlyByteBuf) buf);
    }

    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
        int size = buf.readUnsignedByte();
        this.updates = new byte[size];
        this.heights = new int[size];
        for (int i = 0; i < size; i++) {
            this.updates[i] = buf.readByte();
            this.heights[i] = VarInt.read(buf);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.chunkPos.x);
        buf.writeInt(this.chunkPos.z);
        buf.writeByte(this.updates.length);
        for (int i = 0; i < this.updates.length; i++) {
            buf.writeByte(this.updates[i] & 0xFF);
            VarInt.write(buf, this.heights[i]);
        }
    }

    /** Look up (x,z) for a packed byte. Public so callers can decode without re-importing. */
    public static int decodeX(int packed) {
        return AddressTools.getLocalX(packed);
    }

    public static int decodeZ(int packed) {
        return AddressTools.getLocalZ(packed);
    }

    public ChunkPos chunkPos() {
        return this.chunkPos;
    }

    public byte[] updates() {
        return this.updates;
    }

    public int[] heights() {
        return this.heights;
    }
}
