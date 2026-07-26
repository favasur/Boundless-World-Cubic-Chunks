package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketCubes
// 1.21: multi-cube payload with cross-loader bit-identical wire format.
public class PacketCubes implements IPacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "multi_cube_data");

    private CubePos[] positions = new CubePos[0];
    private byte[][] sectionBytes = new byte[0][];
    private byte[] skylight = new byte[0];
    private byte[] blocklight = new byte[0];
    private byte[] heightmaps = new byte[0];

    public PacketCubes() {
    }

    public PacketCubes(Collection<Cube> cubes) {
        List<Cube> sorted = new ArrayList<>(cubes);
        sorted.sort(Comparator
                .comparingInt((Cube c) -> c.getCoords().getY())
                .thenComparingInt(c -> c.getCoords().getX())
                .thenComparingInt(c -> c.getCoords().getZ()));
        this.positions = new CubePos[sorted.size()];
        this.sectionBytes = new byte[sorted.size()][];
        this.skylight = new byte[sorted.size() * 2048];
        this.blocklight = new byte[sorted.size() * 2048];
        this.heightmaps = new byte[sorted.size() * 1024];
        int idx = 0;
        for (Cube cube : sorted) {
            this.positions[idx] = cube.getCoords();
            this.sectionBytes[idx] = serializeCube(cube);
            byte[] sky = cube.getSkyLightData() != null ? cube.getSkyLightData() : new byte[2048];
            byte[] block = cube.getBlockLightData() != null ? cube.getBlockLightData() : new byte[2048];
            System.arraycopy(sky, 0, this.skylight, idx * 2048, 2048);
            System.arraycopy(block, 0, this.blocklight, idx * 2048, 2048);
            idx++;
        }
    }

    private static byte[] serializeCube(Cube cube) {
        if (cube.getStorage() == null) return new byte[0];
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        cube.getStorage().getStates().write(buf);
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        buf.release();
        return out;
    }

    public ResourceLocation getId() {
        return ID;
    }

    public static PacketCubes from(FriendlyByteBuf buf) {
        PacketCubes p = new PacketCubes();
        p.readFromBuf(buf);
        return p;
    }

    public static void write(PacketCubes p, FriendlyByteBuf buf) {
        p.write((FriendlyByteBuf) buf);
    }

    /** Source-of-truth reader used by both the StreamCodec and the static helper. */
    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        int count = buf.readUnsignedShort();
        this.positions = new CubePos[count];
        this.sectionBytes = new byte[count][];
        this.skylight = new byte[count * 2048];
        this.blocklight = new byte[count * 2048];
        this.heightmaps = new byte[count * 1024];
        for (int i = 0; i < count; i++) {
            this.positions[i] = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());
            int sectionLen = buf.readInt();
            this.sectionBytes[i] = new byte[sectionLen];
            buf.readBytes(this.sectionBytes[i]);
        }
        buf.readBytes(this.skylight);
        buf.readBytes(this.blocklight);
        buf.readBytes(this.heightmaps);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeShort(this.positions.length);
        for (int i = 0; i < this.positions.length; i++) {
            buf.writeInt(this.positions[i].getX());
            buf.writeInt(this.positions[i].getY());
            buf.writeInt(this.positions[i].getZ());
            buf.writeInt(this.sectionBytes[i].length);
            buf.writeBytes(this.sectionBytes[i]);
        }
        buf.writeBytes(this.skylight);
        buf.writeBytes(this.blocklight);
        buf.writeBytes(this.heightmaps);
    }

    public CubePos[] cubePositions() {
        return this.positions;
    }

    public byte[][] sectionByteArrays() {
        return this.sectionBytes;
    }

    public byte[] skylight() {
        return this.skylight;
    }

    public byte[] blocklight() {
        return this.blocklight;
    }
}
