package io.github.opencubicchunks.cubicchunks.core.network;
import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.AbstractServerMessageHandler
public abstract class AbstractServerMessageHandler<T extends IPacket> extends IPacketHandler<T> {

    @Override
    protected final void handleClientMessage(Level world, Player player, T message) {
        // no-op client-side handler
    }
}
