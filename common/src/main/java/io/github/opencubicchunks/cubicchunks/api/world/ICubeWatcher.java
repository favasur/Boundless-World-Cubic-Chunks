package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.ICubeWatcher
// 1.21: replaces Forge's IMessage with a FriendlyByteBuf payload so common isn't bound to
// SimpleImpl. Each loader relays the call through its own network API.
public interface ICubeWatcher extends XYZAddressable {
    boolean isSentToPlayers();

    @Nullable
    ICube getCube();

    /** Send raw bytes (the IPacket wire format) to all watching players. */
    void sendPacketToAllPlayers(FriendlyByteBuf payload);

    @Override
    int getX();

    @Override
    int getY();

    @Override
    int getZ();

    boolean shouldTick();
}
