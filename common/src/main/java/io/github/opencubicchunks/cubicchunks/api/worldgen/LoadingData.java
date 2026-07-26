package io.github.opencubicchunks.cubicchunks.api.worldgen;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.Objects;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.LoadingData
public class LoadingData<POS> {
    private final POS pos;
    @Nullable
    private CompoundTag nbt;

    public LoadingData(POS pos, @Nullable CompoundTag nbt) {
        this.pos = pos;
        this.nbt = nbt;
    }

    public POS getPos() {
        return this.pos;
    }

    @Nullable
    public CompoundTag getNbt() {
        return this.nbt;
    }

    public void setNbt(CompoundTag tag) {
        this.nbt = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadingData<?> that = (LoadingData<?>) o;
        return this.pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pos);
    }

    @Override
    public String toString() {
        return "LoadingData(" + this.pos + ')';
    }
}
