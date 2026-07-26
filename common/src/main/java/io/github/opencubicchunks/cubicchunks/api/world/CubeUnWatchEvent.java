package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/**
 * Fired when a player stops watching a cube. This 1.21 port replaces the old
 * Forge {@code net.minecraftforge.eventbus.api.Event} base class with a plain
 * POJO so it can be delivered over {@code ICubicPlatform.fireEvent(...)} on
 * either Fabric or NeoForge without a hard dependency on the Forge event
 * bus. Mods that previously listened via {@code @SubscribeEvent} now read it
 * through the platform's fire hook exactly the same way.
 */
public class CubeUnWatchEvent {
    @Nullable
    private final ICube cube;
    private final CubePos cubePos;
    private final ICubeWatcher cubeWatcher;
    private final ServerPlayer player;

    public CubeUnWatchEvent(@Nullable ICube cubeIn, CubePos cubePosIn, ICubeWatcher cubeWatcherIn, ServerPlayer playerIn) {
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
