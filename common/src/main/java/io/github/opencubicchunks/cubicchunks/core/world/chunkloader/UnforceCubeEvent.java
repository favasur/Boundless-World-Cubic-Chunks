package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;

/**
 * Fired when a force-loaded ticket on a cube is released. 1.21 port: drops the
 * Forge {@code Event} base class but keeps the same payload.
 */
public class UnforceCubeEvent {
    public UnforceCubeEvent(Object ticket, CubePos pos) {
    }
}
