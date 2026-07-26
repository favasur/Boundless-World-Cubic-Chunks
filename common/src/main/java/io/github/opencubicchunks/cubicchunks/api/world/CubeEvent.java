package io.github.opencubicchunks.cubicchunks.api.world;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.CubeEvent
public abstract class CubeEvent {
    private final ICube cube;

    public CubeEvent(ICube cube) {
        this.cube = cube;
    }

    public ICube getCube() {
        return this.cube;
    }

    public static class Load extends CubeEvent {
        public Load(ICube cube) {
            super(cube);
        }
    }

    public static class Unload extends CubeEvent {
        public Unload(ICube cube) {
            super(cube);
        }
    }
}
