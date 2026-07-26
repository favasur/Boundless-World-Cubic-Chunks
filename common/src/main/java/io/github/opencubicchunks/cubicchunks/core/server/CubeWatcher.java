package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher
// TODO: integrate with 1.21 player chunk tracking and packet dispatch.
public class CubeWatcher implements XYZAddressable {
    private final PlayerCubeMap playerCubeMap;
    private final CubePos pos;
    private final Set<ServerPlayer> players = new HashSet<>();
    private ICube cube;
    private boolean sent;
    private boolean loading;

    public CubeWatcher(PlayerCubeMap playerCubeMap, CubePos pos) {
        this.playerCubeMap = playerCubeMap;
        this.pos = pos;
    }

    public CubePos getCubePos() {
        return this.pos;
    }

    @Override
    public int getX() {
        return this.pos.getX();
    }

    @Override
    public int getY() {
        return this.pos.getY();
    }

    @Override
    public int getZ() {
        return this.pos.getZ();
    }

    public ICube getCube() {
        return this.cube;
    }

    public void setCube(ICube cube) {
        this.cube = cube;
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
        if (this.cube == null) {
            this.loading = true;
            // TODO: load/generate cube via CubeProviderServer.
            return;
        }
        this.loading = false;
        if (!this.sent) {
            // TODO: send cube data to players.
            this.sent = true;
        }
    }

    public void sendToPlayers() {
        this.sent = true;
    }
}
