package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.CompatHandler
public final class CompatHandler {
    private CompatHandler() {
    }

    public static void init() {
    }

    public static void onCubeLoad(CubeEvent.Load load) {
        // Loader-specific compatibility hooks are applied by loader modules.
    }
}
