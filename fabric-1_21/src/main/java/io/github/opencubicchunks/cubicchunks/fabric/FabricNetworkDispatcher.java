package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.core.network.INetworkDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundCubeDataPacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import io.github.opencubicchunks.cubicchunks.core.network.packet.IPacket;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.UnloadCubePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetworkDispatcher implements INetworkDispatcher {
    @Override
    public void sendToPlayer(ServerPlayer player, IPacket packet) {
        if (packet instanceof ClientboundCubeDataPacket p) {
            ServerPlayNetworking.send(player, new CubeDataPayload(p));
        } else if (packet instanceof ClientboundUnloadCubePacket p) {
            ServerPlayNetworking.send(player, new UnloadCubePayload(p));
        }
    }
}
