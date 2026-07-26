package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.21: Fabric-specific LoaderPlatform shim.
public class FabricCubicPlatform implements ICubicPlatform {

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
        return MinecraftServer.getServer();
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
