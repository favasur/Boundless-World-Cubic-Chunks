package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.common.CubicChunksConstants;
import io.github.opencubicchunks.cubicchunks.core.network.ClientPacketHandler;
import io.github.opencubicchunks.cubicchunks.core.network.payload.ColumnDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeBlockChangePayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.CubeSkyLightPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.HeightMapPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.MultiCubeDataPayload;
import io.github.opencubicchunks.cubicchunks.core.network.payload.UnloadCubePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client entry point. Registers {@link ClientPlayNetworking#registerGlobalReceiver}
 * hooks for every CubicChunks S2C payload declared in {@code common.network.payload}. Each
 * handler defers to the corresponding method in {@link ClientPacketHandler}, the single
 * dispatch target shared by both Fabric and NeoForge.
 */
public class CubicChunksFabricClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CubicChunksConstants.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("CubicChunks Fabric 1.21.x client initializing");

        ClientPlayNetworking.registerGlobalReceiver(CubeDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleCubeData(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(UnloadCubePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleUnloadCube(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(ColumnDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleColumnData(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(MultiCubeDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleMultiCubeData(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(CubeBlockChangePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleCubeBlockChange(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(CubeSkyLightPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleCubeSkyLightUpdates(payload.packet())));

        ClientPlayNetworking.registerGlobalReceiver(HeightMapPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientPacketHandler.handleHeightMapUpdate(payload.packet())));
    }
}
