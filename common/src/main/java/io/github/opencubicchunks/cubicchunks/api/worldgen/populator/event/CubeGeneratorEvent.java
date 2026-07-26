package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;

/**
 * Fired when a cube generator is selected for a world. 1.21 port drops the
 * Forge {@code Event} base class; payload survives intact so existing
 * {@code EventBus.subscribe(CubeGeneratorEvent.class)} callers can keep
 * working through {@code ICubicPlatform.fireEvent}.
 */
public class CubeGeneratorEvent {
    private final ICubeGenerator gen;

    public CubeGeneratorEvent(ICubeGenerator gen) {
        this.gen = gen;
    }

    public ICubeGenerator getGenerator() {
        return this.gen;
    }
}
