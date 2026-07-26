package io.github.opencubicchunks.cubicchunks.core.network;
import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;

import net.minecraft.server.level.ServerPlayer;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher
// 1.21: the legacy SimpleNetworkWrapper reference is replaced by IPacket + a
// loader-specific NetworkDispatcher. Per-loader entry points register their payloads.
public final class PacketDispatcher {

    public static final String CHANNEL = "cubicchunks";

    private PacketDispatcher() {
    }

    public static void sendTo(ServerPlayer player, IPacket packet) {
        if (packet == null) return;
        NetworkDispatcher.sendToPlayer(player, packet);
    }
}
