package io.github.opencubicchunks.cubicchunks.api.worldgen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.HashMap;
import java.util.Map;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase
// 1.21: keeps the abstract shape but strips Forge's IForgeRegistry; the registry itself is
// replaced by a plain static map that loaders boot via Fabric/NeoForge entry points.
public abstract class VanillaCompatibilityGeneratorProviderBase {
    public static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath("cubicchunks", "default");
    private static final Map<ResourceLocation, VanillaCompatibilityGeneratorProviderBase> REGISTRY = new HashMap<>();

    protected ResourceLocation registryName;
    protected String unlocalizedName;

    public VanillaCompatibilityGeneratorProviderBase() {
    }

    public static void init() {
        REGISTRY.clear();
    }

    public static void register(VanillaCompatibilityGeneratorProviderBase provider) {
        if (provider.registryName == null) {
            throw new IllegalArgumentException("Provider has no registry name");
        }
        REGISTRY.put(provider.registryName, provider);
    }

    public static VanillaCompatibilityGeneratorProviderBase get(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get(DEFAULT));
    }

    public VanillaCompatibilityGeneratorProviderBase setRegistryName(ResourceLocation registryNameIn) {
        this.registryName = registryNameIn;
        return this;
    }

    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    public VanillaCompatibilityGeneratorProviderBase setUnlocalizedName(String nameIn) {
        this.unlocalizedName = nameIn;
        return this;
    }

    public String getUnlocalizedName() {
        return this.unlocalizedName;
    }

    public abstract ICubeGenerator provideGenerator(ChunkGenerator vanillaChunkGenerator, Level world);
}
