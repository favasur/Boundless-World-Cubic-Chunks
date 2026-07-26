package io.github.opencubicchunks.cubicchunks.neoforge;

import io.github.opencubicchunks.cubicchunks.common.CubicChunksConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge 26.x entry point.
 * Currently mirrors the 1.21.x implementation. Update for 1.26-specific APIs when available.
 */
@Mod(CubicChunksConstants.MOD_ID)
public class CubicChunksNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(CubicChunksConstants.MOD_ID);

    public CubicChunksNeoForge(IEventBus modBus) {
        LOGGER.info("CubicChunks NeoForge 26.x initializing");
        modBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // TODO: register registries, packets, and cubic chunk init hooks
    }
}
