package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUnloadCubePacket implements IPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "unload_cube");

    private final CubePos pos;

    public ClientboundUnloadCubePacket(CubePos pos) {
        this.pos = pos;
    }

    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        read(buf);
    }

    public static ClientboundUnloadCubePacket read(FriendlyByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        return new ClientboundUnloadCubePacket(CubePos.of(x, y, z));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
    }

    public CubePos getPos() {
        return this.pos;
    }
}
