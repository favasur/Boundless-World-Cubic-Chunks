package io.github.opencubicchunks.cubicchunks.fabric;

import io.github.opencubicchunks.cubicchunks.common.CubicChunksConstants;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric 26.x entry point.
 * Currently mirrors the 1.21.x implementation. Update for 1.26-specific APIs when available.
 */
public class CubicChunksFabric implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CubicChunksConstants.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("CubicChunks Fabric 26.x initializing");
        // TODO: register packets, events, and cubic chunk init hooks
    }
}
