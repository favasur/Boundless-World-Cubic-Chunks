package io.github.opencubicchunks.cubicchunks.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-agnostic packet payload. Loader modules register handlers by {@link #getId()}.
 */
public interface IPacket {
    ResourceLocation getId();

    void write(FriendlyByteBuf buf);

    void readFromBuf(FriendlyByteBuf buf);
}
