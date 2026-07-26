package io.github.opencubicchunks.cubicchunks.api.world;

import net.minecraft.nbt.CompoundTag;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent
public class CubeDataEvent extends CubeEvent {
    private final CompoundTag data;

    public CubeDataEvent(ICube cube, CompoundTag data) {
        super(cube);
        this.data = data;
    }

    public CompoundTag getData() {
        return this.data;
    }

    public static class Load extends CubeDataEvent {
        public Load(ICube cube, CompoundTag data) {
            super(cube, data);
        }
    }

    public static class Save extends CubeDataEvent {
        public Save(ICube cube, CompoundTag data) {
            super(cube, data);
        }
    }
}
