package io.github.opencubicchunks.cubicchunks.core.network.packet;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;

public class ClientboundCubeDataPacket implements IPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cubicchunks", "cube_data");

    private final CubePos pos;
    private final byte[] sectionBytes;
    private final CompoundTag[] blockEntityTags;
    private final CompoundTag[] entityTags;
    private final boolean populated;
    private final boolean fullyPopulated;
    private final boolean surfaceTracked;
    private final boolean initialLightingDone;

    public ClientboundCubeDataPacket(
            CubePos pos,
            byte[] sectionBytes,
            CompoundTag[] blockEntityTags,
            CompoundTag[] entityTags,
            boolean populated,
            boolean fullyPopulated,
            boolean surfaceTracked,
            boolean initialLightingDone
    ) {
        this.pos = pos;
        this.sectionBytes = sectionBytes;
        this.blockEntityTags = blockEntityTags;
        this.entityTags = entityTags;
        this.populated = populated;
        this.fullyPopulated = fullyPopulated;
        this.surfaceTracked = surfaceTracked;
        this.initialLightingDone = initialLightingDone;
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

        buf.writeInt(this.sectionBytes.length);
        buf.writeBytes(this.sectionBytes);

        buf.writeInt(this.blockEntityTags.length);
        for (CompoundTag tag : this.blockEntityTags) {
            buf.writeNbt(tag);
        }

        buf.writeInt(this.entityTags.length);
        for (CompoundTag tag : this.entityTags) {
            buf.writeNbt(tag);
        }

        buf.writeBoolean(this.populated);
        buf.writeBoolean(this.fullyPopulated);
        buf.writeBoolean(this.surfaceTracked);
        buf.writeBoolean(this.initialLightingDone);
    }

    @Override
    public void readFromBuf(FriendlyByteBuf buf) {
        read(buf);
    }

    public static ClientboundCubeDataPacket read(FriendlyByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        CubePos pos = CubePos.of(x, y, z);

        int sectionLen = buf.readInt();
        byte[] sectionBytes = new byte[sectionLen];
        buf.readBytes(sectionBytes);

        int blockEntityCount = buf.readInt();
        CompoundTag[] blockEntityTags = new CompoundTag[blockEntityCount];
        for (int i = 0; i < blockEntityCount; i++) {
            blockEntityTags[i] = buf.readNbt();
        }

        int entityCount = buf.readInt();
        CompoundTag[] entityTags = new CompoundTag[entityCount];
        for (int i = 0; i < entityCount; i++) {
            entityTags[i] = buf.readNbt();
        }

        boolean populated = buf.readBoolean();
        boolean fullyPopulated = buf.readBoolean();
        boolean surfaceTracked = buf.readBoolean();
        boolean initialLightingDone = buf.readBoolean();

        return new ClientboundCubeDataPacket(pos, sectionBytes, blockEntityTags, entityTags, populated, fullyPopulated, surfaceTracked, initialLightingDone);
    }

    public CubePos getPos() { return this.pos; }

    public byte[] getSectionBytes() { return this.sectionBytes; }

    public CompoundTag[] getBlockEntityTags() { return this.blockEntityTags; }

    public CompoundTag[] getEntityTags() { return this.entityTags; }

    public boolean isPopulated() { return this.populated; }

    public boolean isFullyPopulated() { return this.fullyPopulated; }

    public boolean isSurfaceTracked() { return this.surfaceTracked; }

    public boolean isInitialLightingDone() { return this.initialLightingDone; }
}
