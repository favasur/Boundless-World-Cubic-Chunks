package io.github.opencubicchunks.cubicchunks.neoforge;

import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

// @Original: 1.21: NeoForge-specific LoaderPlatform shim. Adds fireEvent so common.go
// code can publish events without referencing Forge-only classes.
public class NeoForgeCubicPlatform implements ICubicPlatform {

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public String sideName() {
        return isClient() ? "neoforge-client" : "neoforge-server";
    }

    @Override
    public BlockableEventLoop<?> mainThreadExecutor() {
        if (isClient()) return Minecraft.getInstance();
        return MinecraftServer.getServer();
    }

    @Nullable
    @Override
    public Level getClientLevel() {
        if (isClient()) return Minecraft.getInstance().level;
        return null;
    }

    @Override
    public void fireEvent(Object event) {
        NeoForge.EVENT_BUS.post(event);
    }
}
