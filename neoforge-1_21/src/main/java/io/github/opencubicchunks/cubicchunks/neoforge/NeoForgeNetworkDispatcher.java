package io.github.opencubicchunks.cubicchunks.neoforge;

import io.github.opencubicchunks.cubicchunks.core.network.INetworkDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundCubeDataPacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.UnloadCubePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetworkDispatcher implements INetworkDispatcher {
    @Override
    public void sendToPlayer(ServerPlayer player, IPacket packet) {
        if (packet instanceof ClientboundCubeDataPacket p) {
            PacketDistributor.sendToPlayer(player, new CubeDataPayload(p));
        } else if (packet instanceof ClientboundUnloadCubePacket p) {
            PacketDistributor.sendToPlayer(player, new UnloadCubePayload(p));
        }
    }
}
