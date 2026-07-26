package io.github.opencubicchunks.cubicchunks.neoforge;

import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import org.jetbrains.annotations.Nullable;

// @Original: 1.21: NeoForge-specific LoaderPlatform shim. Adds fireEvent so common.go
// code can publish events without referencing Forge-only classes.
public class NeoForgeCubicPlatform implements ICubicPlatform {

    // 1.21.x removed MinecraftServer.getServer(); the platform holds the reference
    // captured from ServerStartedEvent and exposed via setServer().
    private static volatile @Nullable MinecraftServer SERVER;

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }

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
        MinecraftServer s = SERVER;
        if (s == null) throw new IllegalStateException("Server not started yet; NeoForgeCubicPlatform.setServer() must be called from ServerStartedEvent before mainThreadExecutor() runs on the server side.");
        return s;
    }

    @Nullable
    @Override
    public Level getClientLevel() {
        if (isClient()) return Minecraft.getInstance().level;
        return null;
    }

    @Override
    public void fireEvent(Object event) {
        if (event instanceof net.neoforged.bus.api.Event ne) {
            NeoForge.EVENT_BUS.post(ne);
        }
    }
}
