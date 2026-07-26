package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.network.NetworkDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketEncoder;
import io.github.opencubicchunks.cubicchunks.core.network.packet.ClientboundUnloadCubePacket;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap
// 1.21: per-player 3D cube tracking. Sends cube data when a cube enters the
// player's view distance and removes the player ticket when it leaves.
public class PlayerCubeMap {
    private final ServerLevel level;
    private final CubeProviderServer provider;
    private final Map<UUID, Set<CubePos>> trackedCubes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<CubePos>> pendingLoads = new ConcurrentHashMap<>();
    private final Map<CubePos, Set<UUID>> cubeToPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerTicket> playerTickets = new ConcurrentHashMap<>();
    private int viewDistance = 8;

    public PlayerCubeMap(ServerLevel level, CubeProviderServer provider) {
        this.level = level;
        this.provider = provider;
    }

    /**
     * Sets the cubic view distance. The same value is used horizontally and vertically,
     * so a sphere/cube of {@code distance} cubes is loaded around each player.
     */
    public void setViewDistance(int distance) {
        this.viewDistance = distance;
    }

    public void tick() {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : this.level.players()) {
            online.add(player.getUUID());
            this.updatePlayer(player);
        }

        // Clean up players that have gone offline.
        Iterator<UUID> it = new HashSet<>(this.trackedCubes.keySet()).iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (!online.contains(id)) {
                this.removePlayerById(id);
            }
        }
    }

    private void updatePlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        Set<CubePos> current = this.trackedCubes.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet());
        Set<CubePos> pending = this.pendingLoads.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet());
        Set<CubePos> desired = this.computeDesiredCubes(player);

        // Send unload for cubes that left the player's view distance.
        for (CubePos pos : new HashSet<>(current)) {
            if (!desired.contains(pos)) {
                NetworkDispatcher.sendToPlayer(player, new ClientboundUnloadCubePacket(pos));
                current.remove(pos);
                pending.remove(pos);
                this.removePlayerFromCube(id, pos);
            }
        }

        // Load and send cubes that entered the view distance.
        for (CubePos pos : desired) {
            if (!current.contains(pos) && pending.add(pos)) {
                this.provider.getCubeFuture(pos.getX(), pos.getY(), pos.getZ(), CubeProviderServer.Requirement.LIGHT)
                        .thenAccept(cube -> this.sendCube(player, cube, current, pending, desired, pos));
            }
        }
    }

    private void sendCube(ServerPlayer player, Cube cube, Set<CubePos> current, Set<CubePos> pending, Set<CubePos> desired, CubePos pos) {
        pending.remove(pos);
        if (cube == null || current.contains(pos)) {
            return;
        }
        if (!desired.contains(pos)) {
            return;
        }
        current.add(pos);
        this.addPlayerToCube(player, pos, cube);
        NetworkDispatcher.sendToPlayer(player, PacketEncoder.encodeCube(cube));
    }

    private void addPlayerToCube(ServerPlayer player, CubePos pos, Cube cube) {
        UUID id = player.getUUID();
        this.cubeToPlayers.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet()).add(id);
        PlayerTicket ticket = this.playerTickets.computeIfAbsent(id, PlayerTicket::new);
        cube.getTickets().add(ticket);
    }

    private void removePlayerFromCube(UUID playerId, CubePos pos) {
        Set<UUID> players = this.cubeToPlayers.get(pos);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                this.cubeToPlayers.remove(pos);
            }
        }
        PlayerTicket ticket = this.playerTickets.get(playerId);
        if (ticket == null) {
            return;
        }
        Cube cube = this.provider.getLoadedCube(pos);
        if (cube != null) {
            cube.getTickets().remove(ticket);
            this.provider.tryUnloadCube(cube);
        }
    }

    private void removePlayerById(UUID playerId) {
        Set<CubePos> current = this.trackedCubes.remove(playerId);
        if (current != null) {
            for (CubePos pos : current) {
                this.removePlayerFromCube(playerId, pos);
            }
        }
        this.playerTickets.remove(playerId);
        this.pendingLoads.remove(playerId);
    }

    public void removePlayer(ServerPlayer player) {
        this.removePlayerById(player.getUUID());
    }

    /**
     * Called when a cube is created on-demand (e.g., a player placed a block in empty space).
     * Sends the new cube data to every player currently tracking its position.
     */
    public void onCubeCreated(Cube cube) {
        CubePos pos = cube.getCoords();
        Set<UUID> players = this.cubeToPlayers.get(pos);
        if (players != null && !players.isEmpty()) {
            for (ServerPlayer player : this.level.players()) {
                if (players.contains(player.getUUID())) {
                    NetworkDispatcher.sendToPlayer(player, PacketEncoder.encodeCube(cube));
                }
            }
            return;
        }
        // Freshly created cubes have no trackers yet. Notify nearby players so they see
        // blocks placed by other players.
        for (ServerPlayer player : this.level.players()) {
            if (this.isWithinViewDistance(player, pos)) {
                NetworkDispatcher.sendToPlayer(player, PacketEncoder.encodeCube(cube));
            }
        }
    }

    private boolean isWithinViewDistance(ServerPlayer player, CubePos pos) {
        int dx = Math.abs(Coords.blockToCube(player.getBlockX()) - pos.getX());
        int dy = Math.abs(Coords.blockToCube(player.getBlockY()) - pos.getY());
        int dz = Math.abs(Coords.blockToCube(player.getBlockZ()) - pos.getZ());
        // Use the same sphere as computeDesiredCubes for consistency.
        return dx * dx + dy * dy + dz * dz <= this.viewDistance * this.viewDistance;
    }

    private Set<CubePos> computeDesiredCubes(ServerPlayer player) {
        Set<CubePos> desired = ConcurrentHashMap.newKeySet();
        int cubeX = Coords.blockToCube(player.getBlockX());
        int cubeY = Coords.blockToCube(player.getBlockY());
        int cubeZ = Coords.blockToCube(player.getBlockZ());
        int dist = this.viewDistance;
        for (int x = -dist; x <= dist; x++) {
            for (int z = -dist; z <= dist; z++) {
                for (int y = -dist; y <= dist; y++) {
                    if (x * x + y * y + z * z <= dist * dist) {
                        desired.add(CubePos.of(cubeX + x, cubeY + y, cubeZ + z));
                    }
                }
            }
        }
        return desired;
    }

    private static final class PlayerTicket implements ITicket {
        private final UUID playerId;

        private PlayerTicket(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public boolean shouldTick() {
            return false;
        }
    }
}
