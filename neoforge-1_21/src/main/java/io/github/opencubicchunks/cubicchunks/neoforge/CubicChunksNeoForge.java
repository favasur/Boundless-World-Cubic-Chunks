package io.github.opencubicchunks.cubicchunks.neoforge;

import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.common.CubicChunksConstants;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.network.ClientPacketHandler;
import io.github.opencubicchunks.cubicchunks.core.network.NetworkDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.payload.ColumnDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeBlockChangePayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeSkyLightPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.HeightMapPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.MultiCubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.UnloadCubePayload;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CubicChunksConstants.MOD_ID)
public class CubicChunksNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(CubicChunksConstants.MOD_ID);

    public CubicChunksNeoForge(IEventBus modBus) {
        LOGGER.info("CubicChunks NeoForge 1.21.x initializing");
        ICubicPlatform.Holder.set(new NeoForgeCubicPlatform());
        modBus.addListener(this::setup);
        modBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NetworkDispatcher.setInstance(new NeoForgeNetworkDispatcher());
    }

    private void onServerStarted(ServerStartedEvent event) {
        // 1.21.x removed MinecraftServer.getServer(); capture the reference here
        // so NeoForgeCubicPlatform.mainThreadExecutor() can return it on the server side.
        NeoForgeCubicPlatform.setServer(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        // Without this, the static SERVER field still points at the dead MinecraftServer
        // after a world closes. A subsequent server-start would then hand callers a
        // stopped instance from a prior run, and mainThreadExecutor() would silently
        // run tasks against an unbound thread loop.
        NeoForgeCubicPlatform.setServer(null);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ICubeProvider provider = ((ICubicWorldInternal) level).getCubeCache();
        if (provider instanceof CubeProviderServer serverProvider) {
            serverProvider.getPlayerCubeMap().removePlayer(player);
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        if (CubicChunksConfig.stackingDimensionsEnabled) {
            StackedDimensionRegistry.bootDefaultsIfNeeded();
            // Mark the overworld as cubic so MixinServerLevel actually initialises
            // its CubeProviderServer (and therefore StackedCubeGenerator) for it.
            // Without this, the early-return in cc$init keeps stacking dormant.
            CubicChunks.registerCubicDimension(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
            LOGGER.info("Stacked dimensions registered: {} (overworld marked cubic)",
                    StackedDimensionRegistry.size());
        }
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CubicChunksConstants.MOD_ID);
        registrar.playToClient(
                CubeDataPayload.TYPE,
                CubeDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleCubeData(payload.packet()))
        );
        registrar.playToClient(
                UnloadCubePayload.TYPE,
                UnloadCubePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleUnloadCube(payload.packet()))
        );
        registrar.playToClient(
                ColumnDataPayload.TYPE, ColumnDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleColumnData(payload.packet()))
        );
        registrar.playToClient(
                MultiCubeDataPayload.TYPE, MultiCubeDataPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleMultiCubeData(payload.packet()))
        );
        registrar.playToClient(
                CubeBlockChangePayload.TYPE, CubeBlockChangePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleCubeBlockChange(payload.packet()))
        );
        registrar.playToClient(
                CubeSkyLightPayload.TYPE, CubeSkyLightPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleCubeSkyLightUpdates(payload.packet()))
        );
        registrar.playToClient(
                HeightMapPayload.TYPE, HeightMapPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandler.handleHeightMapUpdate(payload.packet()))
        );
    }
}
