package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-specific implementation of network dispatching.
 */
public interface INetworkDispatcher {
    void sendToPlayer(ServerPlayer player, IPacket packet);
}
