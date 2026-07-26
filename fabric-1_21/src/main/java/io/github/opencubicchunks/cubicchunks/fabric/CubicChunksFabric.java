package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.common.CubicChunksConstants;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubicChunksFabric implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CubicChunksConstants.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("CubicChunks Fabric 1.21.x initializing");
        ICubicPlatform.Holder.set(new FabricCubicPlatform());
        NetworkDispatcher.setInstance(new FabricNetworkDispatcher());

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

        PayloadTypeRegistry.playS2C().register(CubeDataPayload.TYPE, CubeDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(UnloadCubePayload.TYPE, UnloadCubePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ColumnDataPayload.TYPE, ColumnDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MultiCubeDataPayload.TYPE, MultiCubeDataPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CubeBlockChangePayload.TYPE, CubeBlockChangePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CubeSkyLightPayload.TYPE, CubeSkyLightPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HeightMapPayload.TYPE, HeightMapPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel level = player.serverLevel();
            ICubeProvider provider = ((ICubicWorldInternal) level).getCubeCache();
            if (provider instanceof CubeProviderServer serverProvider) {
                serverProvider.getPlayerCubeMap().removePlayer(player);
            }
        });
    }
}
