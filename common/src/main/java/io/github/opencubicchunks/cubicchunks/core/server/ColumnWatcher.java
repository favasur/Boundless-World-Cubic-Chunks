package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher
// TODO: integrate with 1.21 player chunk tracking and packet dispatch.
public class ColumnWatcher implements XZAddressable {
    private final PlayerCubeMap playerCubeMap;
    private final ChunkPos pos;
    private final Set<ServerPlayer> players = new HashSet<>();
    private boolean sent;
    private boolean loading;

    public ColumnWatcher(PlayerCubeMap playerCubeMap, ChunkPos pos) {
        this.playerCubeMap = playerCubeMap;
        this.pos = pos;
    }

    public ChunkPos getChunkPos() {
        return this.pos;
    }

    @Override
    public int getX() {
        return this.pos.x;
    }

    @Override
    public int getZ() {
        return this.pos.z;
    }

    public boolean isSent() {
        return this.sent;
    }

    public void addPlayer(ServerPlayer player) {
        this.players.add(player);
    }

    public void removePlayer(ServerPlayer player) {
        this.players.remove(player);
    }

    public boolean hasPlayers() {
        return !this.players.isEmpty();
    }

    public int getDistance() {
        // TODO: compute distance to nearest player.
        return Integer.MAX_VALUE;
    }

    public void update() {
        if (!this.sent) {
            // TODO: send column data to players.
            this.sent = true;
        }
    }

    public void sendToPlayers() {
        this.sent = true;
    }
}
