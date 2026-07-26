package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public class NetworkDispatcher {
    @Nullable
    private static INetworkDispatcher instance;

    public static void setInstance(INetworkDispatcher instance) {
        NetworkDispatcher.instance = instance;
    }

    public static void sendToPlayer(ServerPlayer player, IPacket packet) {
        if (instance != null) {
            instance.sendToPlayer(player, packet);
        }
    }
}
