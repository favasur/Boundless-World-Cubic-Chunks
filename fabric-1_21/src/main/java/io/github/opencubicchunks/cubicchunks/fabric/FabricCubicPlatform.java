package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

// @Original: 1.21: Fabric-specific LoaderPlatform shim.
public class FabricCubicPlatform implements ICubicPlatform {

    // 1.21.x removed MinecraftServer.getServer(); the platform holds the reference
    // captured from ServerLifecycleEvents.SERVER_STARTED and exposed via setServer().
    private static volatile @Nullable MinecraftServer SERVER;

    public static void setServer(MinecraftServer server) {
        SERVER = server;
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public String sideName() {
        return isClient() ? "fabric-client" : "fabric-server";
    }

    @Override
    public BlockableEventLoop<?> mainThreadExecutor() {
        if (isClient()) return Minecraft.getInstance();
        MinecraftServer s = SERVER;
        if (s == null) throw new IllegalStateException("Server not started yet; FabricCubicPlatform.setServer() must be called from ServerLifecycleEvents.SERVER_STARTED before mainThreadExecutor() runs on the server side.");
        return s;
    }

    @Nullable
    @Override
    public Level getClientLevel() {
        if (isClient()) {
            return Minecraft.getInstance().level;
        }
        return null;
    }
}
