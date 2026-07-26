package io.github.opencubicchunks.cubicchunks.core.network;
import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.network.AbstractMessageHandler
// 1.21: reorganized to a single abstract type with side-specialized subclasses.
public abstract class IPacketHandler<T extends IPacket> {

    protected abstract void handleClientMessage(Level world, @Nullable Player player, T message);

    protected abstract void handleServerMessage(Player player, T message);

    public final void dispatch(Level world, @Nullable Player player, T message) {
        if (message == null) return;
        try {
            if (world == null) {
                net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal(
                        "CubicChunks: dropped packet " + message.getClass().getSimpleName() + " – world unavailable");
                if (player != null) {
                    player.displayClientMessage(text, false);
                }
                return;
            }
            if (world.isClientSide()) {
                handleClientMessage(world, player, message);
            } else {
                if (player == null) return;
                handleServerMessage(player, message);
            }
        } catch (Throwable t) {
            io.github.opencubicchunks.cubicchunks.core.CubicChunks.LOGGER.error("Error handling packet {}", message.getClass().getSimpleName(), t);
            throw t;
        }
    }
}
