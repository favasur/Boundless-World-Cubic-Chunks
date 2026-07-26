package io.github.opencubicchunks.cubicchunks.api.world;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight
public interface IMinMaxHeight {
    default int getMinHeight() {
        return 0;
    }

    default int getMaxHeight() {
        return 256;
    }
}
