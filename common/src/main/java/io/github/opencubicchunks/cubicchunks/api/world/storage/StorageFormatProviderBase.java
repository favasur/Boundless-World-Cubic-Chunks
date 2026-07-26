package io.github.opencubicchunks.cubicchunks.api.world.storage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase
public abstract class StorageFormatProviderBase {
    public static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath("cubicchunks", "anvil3d");
    private static final Map<ResourceLocation, StorageFormatProviderBase> REGISTRY = new HashMap<>();

    protected ResourceLocation registryName;
    protected String unlocalizedName;

    public StorageFormatProviderBase() {
    }

    public static void init() {
        REGISTRY.clear();
    }

    public static void register(StorageFormatProviderBase provider) {
        if (provider.registryName == null)
            throw new IllegalArgumentException("Storage format provider has no registry name");
        REGISTRY.put(provider.registryName, provider);
    }

    public static StorageFormatProviderBase get(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get(DEFAULT));
    }

    public static ResourceLocation defaultStorageFormatProviderName(String fallback) {
        return REGISTRY.get(DEFAULT) != null || fallback.isEmpty()
                ? DEFAULT
                : (fallback.contains(":")
                        ? ResourceLocation.tryParse(fallback)
                        : ResourceLocation.fromNamespaceAndPath("minecraft", fallback));
    }

    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    public StorageFormatProviderBase setRegistryName(ResourceLocation registryNameIn) {
        this.registryName = registryNameIn;
        return this;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public StorageFormatProviderBase setUnlocalizedName(String nameIn) {
        this.unlocalizedName = nameIn;
        return this;
    }

    public abstract ICubicStorage provideStorage(Level world, Path path) throws IOException;

    public boolean canBeDefault() {
        return false;
    }
}
