package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.client.ClientEventHandler
// 1.21: client lifecycle hook. Most client behaviour is implemented via mixins
// MixinClientLevel and MixinViewArea. This class stays registered via each loader's
// entry point so future renderer-level hooks have a place to land.
public class ClientEventHandler {

    public static void init() {
        CubicChunks.LOGGER.info("CubicChunks client event handler initialised");
    }

    public static void onClientTick() {
        // placeholder; per-cube sync runs through the cube provider's tick path.
    }
}
