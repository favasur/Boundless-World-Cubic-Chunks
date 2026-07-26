package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler
// 1.21: loader-agnostic flat-tracker for the allow-vanilla-clients scenario. The
// original method set spans Forge's SxxPacket quirks and is not portable to a vanilla
// 1.21 stack; in 1.21 we only run this when allowVanillaClients=false (default), so
// most of the runtime state is dormant. The shape is preserved for plugin compatibility.
public class VanillaNetworkHandler {

    private final ServerLevel world;
    private final Map<ServerPlayer, CubePos> playerOffsets = new IdentityHashMap<>();
    private final Map<ServerPlayer, CubePos> playerOffsetsC2S = new IdentityHashMap<>();
    private final Map<ServerPlayer, Integer> expectedTeleportId = new IdentityHashMap<>();
    private final Set<UUID> bedrockPlayers = new HashSet<>();

    public VanillaNetworkHandler(ServerLevel world) {
        this.world = world;
    }

    public boolean hasCubicChunks(ServerPlayer player) {
        return true;
    }

    public BlockPos getS2COffset(ServerPlayer player) {
        return BlockPos.ZERO;
    }

    public BlockPos getC2SOffset(ServerPlayer player) {
        return BlockPos.ZERO;
    }

    public boolean receiveOffsetUpdateConfirm(ServerPlayer player, int teleportId) {
        return true;
    }

    public void removePlayer(ServerPlayer player) {
        playerOffsets.remove(player);
        playerOffsetsC2S.remove(player);
        expectedTeleportId.remove(player);
        bedrockPlayers.remove(player.getUUID());
    }

    public static void addBedrockPlayer(ServerPlayer player) { }

    public static void removeBedrockPlayer(ServerPlayer player) { }
}
