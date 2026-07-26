package io.github.opencubicchunks.cubicchunks.core;

// @Original: 1.12.2:io.github.opencubicchunks.core.CommonEventHandler
// 1.21: lightweight loader-agnostic handler. Each mod entry point calls initCommon()
// and onLoad(). Specific NeoForge @SubscribeEvent and Fabric @EventListener methods
// live in their respective loader modules.
public class CommonEventHandler {

    public static void initCommon() {
        CubicChunks.LOGGER.info("CubicChunks common initialized");
    }

    public static void onLoad() {
        // 1.21 BootstrapContext registration now happens in loader-specific entry points.
    }
}
