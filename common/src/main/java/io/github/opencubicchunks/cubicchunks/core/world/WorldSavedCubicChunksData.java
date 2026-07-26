package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData
public class WorldSavedCubicChunksData extends SavedData {
    public boolean isCubicChunks = false;
    public int minHeight = 0;
    public int maxHeight = 256;
    public ResourceLocation compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
    public ResourceLocation storageFormat = StorageFormatProviderBase.DEFAULT;

    public WorldSavedCubicChunksData() {
        super();
    }

    public WorldSavedCubicChunksData(boolean isCC, int min, int max) {
        super();
        if (isCC) {
            this.minHeight = min;
            this.maxHeight = max;
            this.isCubicChunks = true;
            this.compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
            this.storageFormat = StorageFormatProviderBase.DEFAULT;
        }
    }

    
    public CompoundTag save(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries) {
        compound.putInt("minHeight", this.minHeight);
        compound.putInt("maxHeight", this.maxHeight);
        compound.putBoolean("isCubicChunks", this.isCubicChunks);
        compound.putString("compatibilityGeneratorType", this.compatibilityGeneratorType.toString());
        compound.putString("storageFormat", this.storageFormat.toString());
        return compound;
    }

    public static WorldSavedCubicChunksData load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        WorldSavedCubicChunksData data = new WorldSavedCubicChunksData();
        data.minHeight = nbt.getInt("minHeight");
        data.maxHeight = nbt.getInt("maxHeight");
        data.isCubicChunks = !nbt.contains("isCubicChunks") || nbt.getBoolean("isCubicChunks");
        if (nbt.contains("compatibilityGeneratorType")) {
            data.compatibilityGeneratorType = ResourceLocation.tryParse(nbt.getString("compatibilityGeneratorType"));
        } else {
            data.compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
        }
        if (nbt.contains("storageFormat")) {
            data.storageFormat = ResourceLocation.tryParse(nbt.getString("storageFormat"));
        } else {
            data.storageFormat = StorageFormatProviderBase.DEFAULT;
        }
        return data;
    }
}
