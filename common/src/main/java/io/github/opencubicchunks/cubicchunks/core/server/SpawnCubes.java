package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes
public class SpawnCubes implements ITicket {
    @Nullable private BlockPos spawnPoint = null;
    private int radiusXZGenerate = 8;
    private int radiusYGenerate = 4;
    private int radiusXZForce = 4;
    private int radiusYForce = 2;

    public SpawnCubes() {
    }

    public void update(Level world) {
        update(world, 8, 4, 4, 2);
    }

    public void update(Level world, int newXZGen, int newYGen, int newXZForce, int newYForce) {
        if (this.spawnPoint == null
                || this.radiusXZGenerate != newXZGen
                || this.radiusYGenerate != newYGen
                || this.radiusXZForce != newXZForce
                || this.radiusYForce != newYForce) {
            removeTickets(world);
            this.spawnPoint = world.getSharedSpawnPos();
            this.radiusXZGenerate = newXZGen;
            this.radiusYGenerate = newYGen;
            this.radiusXZForce = newXZForce;
            this.radiusYForce = newYForce;
            addTickets(world);
        }
    }

    private void removeTickets(Level world) {
        if (this.radiusYForce >= 0 && this.radiusXZForce >= 0 && this.spawnPoint != null && world instanceof net.minecraft.server.level.ServerLevel level) {
            var cache = (io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal) level.getChunkSource();
            int sx = Coords.blockToCube(this.spawnPoint.getX());
            int sy = Coords.blockToCube(this.spawnPoint.getY());
            int sz = Coords.blockToCube(this.spawnPoint.getZ());
            for (int x = sx - this.radiusXZForce; x <= sx + this.radiusXZForce; x++) {
                for (int z = sz - this.radiusXZForce; z <= sz + this.radiusXZForce; z++) {
                    for (int y = sy + this.radiusYForce; y >= sy - this.radiusYForce; y--) {
                        cache.getCube(x, y, z).getTickets().remove(this);
                    }
                }
            }
        }
    }

    private void addTickets(Level world) {
        if (this.radiusXZGenerate >= 0 && this.radiusYGenerate >= 0 && this.spawnPoint != null
                && world instanceof net.minecraft.server.level.ServerLevel level) {
            var cache = (CubeProviderServer) ((io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal) level).getCubeCache();
            CubicChunks.LOGGER.info("Loading cubes for spawn...");
            int sx = Coords.blockToCube(this.spawnPoint.getX());
            int sy = Coords.blockToCube(this.spawnPoint.getY());
            int sz = Coords.blockToCube(this.spawnPoint.getZ());
            int r = Math.max(this.radiusXZGenerate, this.radiusXZForce);
            int ry = Math.max(this.radiusYGenerate, this.radiusYForce);
            for (int cx = sx - r; cx <= sx + r; cx++) {
                for (int cz = sz - r; cz <= sz + r; cz++) {
                    for (int cy = sy + ry; cy >= sy - ry; cy--) {
                        int dx = Math.abs(cx - sx);
                        int dy = Math.abs(cy - sy);
                        int dz = Math.abs(cz - sz);
                        ICubeProviderServer.Requirement req =
                                (dx < this.radiusXZGenerate && dz < this.radiusXZGenerate && dy < this.radiusYGenerate)
                                        ? ICubeProviderServer.Requirement.LIGHT
                                        : ICubeProviderServer.Requirement.GENERATE;
                        Cube cube = cache.getCubeNow(cx, cy, cz, req);
                        if (cube != null && dx <= this.radiusXZForce && dz <= this.radiusXZForce) {
                            cube.getTickets().add(this);
                        }
                    }
                }
            }
        }
    }

    @Override public boolean shouldTick() { return false; }
}
