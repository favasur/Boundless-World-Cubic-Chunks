package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;

/**
 * Fired when a cube is force-loaded. 1.21 port: drops the Forge event bus and
 * keeps the same payload so listeners (NeoForge-specific mods) still get the
 * message through {@code ICubicPlatform.fireEvent}.
 */
public class ForceCubeEvent {
    public ForceCubeEvent(Object ticket, CubePos pos) {
    }
}
