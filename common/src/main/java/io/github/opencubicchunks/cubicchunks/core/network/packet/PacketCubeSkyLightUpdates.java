package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketCubeSkyLightUpdates
// 1.21: bulk sky-light update packet. Uses LevelChunkSection.getSkyLight() / setData().
public class PacketCubeSkyLightUpdates implements IPacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "cube_sky_light");

    private CubePos cubePos = CubePos.of(0, 0, 0);
    private boolean fullRelight;
    private byte[] data = new byte[0];

    public PacketCubeSkyLightUpdates() {
    }

    public PacketCubeSkyLightUpdates(CubePos cubePos, List<Integer> localAddresses, byte[] blockData) {
        this.cubePos = cubePos;
        this.fullRelight = false;
        if (blockData != null) {
            this.data = blockData;
        } else {
            this.data = encodeIncremental(localAddresses);
        }
    }

    private static byte[] encodeIncremental(List<Integer> localAddresses) {
        byte[] out = new byte[localAddresses.size() * 2];
        for (int i = 0; i < localAddresses.size(); i++) {
            int addr = localAddresses.get(i) & 0xFFFF;
            int x = io.github.opencubicchunks.cubicchunks.core.util.AddressTools.getLocalX(addr);
            int y = io.github.opencubicchunks.cubicchunks.core.util.AddressTools.getLocalY(addr);
            int z = io.github.opencubicchunks.cubicchunks.core.util.AddressTools.getLocalZ(addr);
            byte b1 = (byte) (Bits.packUnsignedToInt(x, 4, 0) | Bits.packUnsignedToInt(y, 4, 4));
            byte b2 = (byte) (Bits.packUnsignedToInt(z, 4, 0));
            out[i * 2] = b1;
            out[i * 2 + 1] = b2;
        }
        return out;
    }

    public static PacketCubeSkyLightUpdates forFullRelight(CubePos cubePos, byte[] fullData) {
        PacketCubeSkyLightUpdates p = new PacketCubeSkyLightUpdates();
        p.cubePos = cubePos;
        p.fullRelight = true;
        p.data = fullData != null ? fullData.clone() : new byte[0];
        return p;
    }

    public ResourceLocation getId() {
        return ID;
    }

    public static PacketCubeSkyLightUpdates from(FriendlyByteBuf buf) {
        PacketCubeSkyLightUpdates p = new PacketCubeSkyLightUpdates();
        p.readFromBuf(buf);
        return p;
    }

    public static void write(PacketCubeSkyLightUpdates p, FriendlyByteBuf buf) {
        p.write((FriendlyByteBuf) buf);
    }

    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        this.cubePos = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());
        this.fullRelight = buf.readBoolean();
        boolean hasData = buf.readBoolean();
        if (hasData) {
            int len = VarInt.read(buf);
            this.data = new byte[len];
            buf.readBytes(this.data);
        } else {
            this.data = null;
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.cubePos.getX());
        buf.writeInt(this.cubePos.getY());
        buf.writeInt(this.cubePos.getZ());
        buf.writeBoolean(this.fullRelight);
        buf.writeBoolean(this.data != null);
        if (this.data != null) {
            VarInt.write(buf, this.data.length);
            buf.writeBytes(this.data);
        }
    }

    public CubePos getCubePos() {
        return this.cubePos;
    }

    public boolean isFullRelight() {
        return this.fullRelight;
    }

    public byte[] skyLightData() {
        return this.data;
    }
}
