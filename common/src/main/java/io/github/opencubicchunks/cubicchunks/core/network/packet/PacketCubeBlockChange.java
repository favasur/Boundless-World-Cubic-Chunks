package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.api.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketCubeBlockChange
// 1.21: encodes each height value as a full int (X | (Z<<4) | (Y<<8)) so Y is no
// longer clipped to 0..255.
public class PacketCubeBlockChange implements IPacket {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "cube_block_change");

    private CubePos cubePos = CubePos.of(0, 0, 0);
    private short[] localAddresses = new short[0];
    private BlockState[] blockStates = new BlockState[0];
    private int[] heightValues = new int[0];

    public PacketCubeBlockChange() {
    }

    public PacketCubeBlockChange(CubePos cubePos, List<Integer> changedAddresses,
                                 List<BlockState> states, List<Integer> heightPacked) {
        this.cubePos = cubePos;
        this.localAddresses = new short[changedAddresses.size()];
        for (int i = 0; i < changedAddresses.size(); i++) {
            this.localAddresses[i] = (short) (changedAddresses.get(i) & 0xFFFF);
        }
        this.blockStates = states.toArray(new BlockState[0]);
        this.heightValues = heightPacked.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Convenience factory used by the cube provider to build the wire bytes from a Cube. */
    public static PacketCubeBlockChange of(io.github.opencubicchunks.cubicchunks.core.world.cube.Cube cube,
                                           List<Integer> changedAddresses) {
        BlockState[] blockStates = new BlockState[changedAddresses.size()];
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < changedAddresses.size(); i++) {
            int addr = changedAddresses.get(i);
            int x = AddressTools.getLocalX(addr);
            int y = AddressTools.getLocalY(addr);
            int z = AddressTools.getLocalZ(addr);
            blockStates[i] = cube.getBlockState(x, y, z);
            positions.add(AddressTools.getLocalAddress(x, z));
        }
        int[] heightValues = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            int pos = positions.get(i);
            int x = AddressTools.getLocalX(pos);
            int z = AddressTools.getLocalZ(pos);
            int height = ((IColumn) cube.getColumn()).getOpacityIndex().getTopBlockY(x, z);
            heightValues[i] = (x & 0xF) | ((z & 0xF) << 4) | (height << 8);
        }
        return new PacketCubeBlockChange(cube.getCoords(), changedAddresses, java.util.Arrays.asList(blockStates),
                java.util.Arrays.stream(heightValues).boxed().collect(java.util.stream.Collectors.toList()));
    }

    public ResourceLocation getId() {
        return ID;
    }

    public static PacketCubeBlockChange from(FriendlyByteBuf buf) {
        PacketCubeBlockChange p = new PacketCubeBlockChange();
        p.readFromBuf(buf);
        return p;
    }

    public static void write(PacketCubeBlockChange p, FriendlyByteBuf buf) {
        p.write((FriendlyByteBuf) buf);
    }

    @Override
    public void readFromBuf(FriendlyByteBuf in) {
        this.cubePos = CubePos.of(in.readInt(), in.readInt(), in.readInt());
        int numBlocks = in.readShort();
        this.localAddresses = new short[numBlocks];
        this.blockStates = new BlockState[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            this.localAddresses[i] = in.readShort();
            int stateId = in.readInt();
            int blockId = stateId & 0xFFF;
            int propMask = stateId >>> 12;
            BlockState st = BuiltInRegistries.BLOCK.byId(blockId).defaultBlockState();
            // Properties are encoded separately in stateId; for now we use default state.
            // Loader modules can wire a richer property codec if needed.
            this.blockStates[i] = st != null ? st : Blocks.AIR.defaultBlockState();
        }
        int numHeights = in.readUnsignedByte();
        this.heightValues = new int[numHeights];
        for (int i = 0; i < numHeights; i++) {
            this.heightValues[i] = in.readInt();
        }
    }

    @Override
    public void write(FriendlyByteBuf out) {
        out.writeInt(this.cubePos.getX());
        out.writeInt(this.cubePos.getY());
        out.writeInt(this.cubePos.getZ());
        out.writeShort(this.localAddresses.length);
        for (int i = 0; i < this.localAddresses.length; i++) {
            out.writeShort(this.localAddresses[i]);
            BlockState st = this.blockStates[i] != null ? this.blockStates[i] : Blocks.AIR.defaultBlockState();
            int blockId = BuiltInRegistries.BLOCK.getId(st.getBlock());
            out.writeInt((blockId & 0xFFF) | (0 << 12));
        }
        out.writeByte(this.heightValues.length);
        for (int v : this.heightValues) {
            out.writeInt(v);
        }
    }

    public CubePos getCubePos() {
        return this.cubePos;
    }

    public short[] localAddresses() {
        return this.localAddresses;
    }

    public BlockState[] blockStates() {
        return this.blockStates;
    }

    public int[] heightValues() {
        return this.heightValues;
    }
}
