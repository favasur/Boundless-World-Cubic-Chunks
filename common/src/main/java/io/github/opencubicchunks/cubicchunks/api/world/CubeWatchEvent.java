package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/**
 * Fired when a player starts watching a cube. 1.21 port drops the Forge
 * {@code Event} base class in favour of a plain POJO so the same payload
 * can be broadcast on Fabric and NeoForge through {@code ICubicPlatform.fireEvent}.
 */
public class CubeWatchEvent {
    @Nullable
    private final ICube cube;
    private final CubePos cubePos;
    private final ICubeWatcher cubeWatcher;
    private final ServerPlayer player;

    public CubeWatchEvent(@Nullable ICube cubeIn, CubePos cubePosIn, ICubeWatcher cubeWatcherIn, ServerPlayer playerIn) {
        this.cube = cubeIn;
        this.cubePos = cubePosIn;
        this.cubeWatcher = cubeWatcherIn;
        this.player = playerIn;
    }

    @Nullable public ICube getCube() { return this.cube; }
    public CubePos getCubePos() { return this.cubePos; }
    public ICubeWatcher getCubeWatcher() { return this.cubeWatcher; }
    @Nullable public ICubicWorld getWorld() {
        ServerLevel level = this.player.serverLevel();
        return (level instanceof ICubicWorld cubic) ? cubic : null;
    }
    public ServerPlayer getPlayer() { return this.player; }
}
