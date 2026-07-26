package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundCubeDataPacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;

public class PacketEncoder {
    private PacketEncoder() {
    }

    public static ClientboundCubeDataPacket encodeCube(Cube cube) {
        CubePos pos = cube.getCoords();

        LevelChunkSection storage = cube.getStorage();
        byte[] sectionBytes;
        if (storage != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            storage.write(buf);
            sectionBytes = new byte[buf.readableBytes()];
            buf.readBytes(sectionBytes);
            buf.release();
        } else {
            sectionBytes = new byte[0];
        }

        List<CompoundTag> blockEntityTags = new ArrayList<>();
        for (BlockEntity be : cube.getBlockEntityMap().values()) {
            CompoundTag tag = be.saveWithFullMetadata(cube.getWorld().registryAccess());
            blockEntityTags.add(tag);
        }

        List<CompoundTag> entityTags = new ArrayList<>();
        for (Entity entity : cube.getEntitySet()) {
            CompoundTag tag = new CompoundTag();
            if (entity.save(tag)) {
                entityTags.add(tag);
            }
        }

        return new ClientboundCubeDataPacket(
                pos,
                sectionBytes,
                blockEntityTags.toArray(new CompoundTag[0]),
                entityTags.toArray(new CompoundTag[0]),
                cube.isPopulated(),
                cube.isFullyPopulated(),
                cube.isSurfaceTracked(),
                cube.isInitialLightingDone()
        );
    }

    public static ClientboundUnloadCubePacket encodeUnload(CubePos pos) {
        return new ClientboundUnloadCubePacket(pos);
    }
}
