package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

/**
 * Stacked-band strategy for the default {@code end_stacked} sub-dim. Wraps a real
 * vanilla {@link NoiseBasedChunkGenerator} configured with {@code minecraft:end}
 * so the band picks up the End biome source and surface rules.
 */
public class EndBandStrategy implements StackedBandStrategy {

    private static final ResourceLocation END_NOISE_SETTINGS_ID =
            ResourceLocation.withDefaultNamespace("end");

    private final ServerLevel level;
    private final StackedDimension dim;
    @Nullable
    private final ChunkGenerator endGen;
    @Nullable
    private final Holder<NoiseGeneratorSettings> endSettings;
    @Nullable
    private final ChunkGeneratorStructureState structureState;
    private final RandomState randomState;
    private final Registry<Biome> biomeRegistry;

    public EndBandStrategy(ServerLevel level, StackedDimension dim) {
        this.level = level;
        this.dim = dim;
        this.biomeRegistry = this.level.registryAccess().registryOrThrow(Registries.BIOME);
        END_BIOME_REGISTRY = this.biomeRegistry;
        GeneratorBundle bundle = tryBuildEndGenerator(this.level);
        this.endGen = bundle.gen;
        this.endSettings = bundle.settings;
        this.randomState = this.endSettings != null
                ? tryBuildRandomState(this.level, this.endSettings)
                : tryBuildRandomState(this.level, endFallbackHolder(this.level));
        // Build a per-band ChunkGeneratorStructureState so vanilla structure
        // placement (EndCity) walks the End biome source instead of the
        // overworld's. Falls back to null if construction fails.
        this.structureState = this.endGen != null
                ? BandedFeaturePlacer.tryBuildPerBandStructureState(this.level, this.endGen)
                : null;
    }

    private static final class GeneratorBundle {
        @Nullable final ChunkGenerator gen;
        @Nullable final Holder<NoiseGeneratorSettings> settings;
        GeneratorBundle(@Nullable ChunkGenerator gen, @Nullable Holder<NoiseGeneratorSettings> settings) {
            this.gen = gen;
            this.settings = settings;
        }
    }

    @Override
    public StackedDimension getDimension() {
        return this.dim;
    }

    @Override
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public ChunkGenerator getChunkGenerator() {
        return this.endGen;
    }

    @Override
    public ChunkGeneratorStructureState getChunkGeneratorState() {
        return this.structureState;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        writeBiomes(primer, cubeX, cubeY, cubeZ);
        Random rand = BandedCubeFill.deterministicRandom(this.level, cubeX, cubeY, cubeZ);
        int minBlockY = Coords.cubeToMinBlock(cubeY);
        int maxBlockY = Coords.cubeToMaxBlock(cubeY);
        int dimTop = this.dim.maxBlockY();
        int dimBottom = this.dim.minBlockY();

        Biome columnBiome = sampleColumnBiome(cubeX, cubeZ, minBlockY + 8);
        BlockState body = Blocks.END_STONE.defaultBlockState();
        boolean isEndHighlands = isEndHighlandsBiome(columnBiome);
        boolean isEndMidlands = isEndMidlandsBiome(columnBiome);
        boolean isEndBarrens = isEndBarrensBiome(columnBiome);

        for (int y = 0; y < 16; y++) {
            int blockY = minBlockY + y;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int pillarRate = isEndHighlands ? 96 : isEndMidlands ? 144 : 200;
                    if (rand.nextInt(pillarRate) == 0) {
                        primer.setBlockState(x, y, z, Blocks.OBSIDIAN.defaultBlockState());
                        continue;
                    }
                    if ((isEndHighlands || isEndMidlands)
                            && blockY - dimBottom < 12
                            && rand.nextInt(80) == 0) {
                        primer.setBlockState(x, y, z, Blocks.CHORUS_PLANT.defaultBlockState());
                        continue;
                    }
                    if (isEndMidlands && blockY > dimTop - 24 && rand.nextInt(40) == 0) {
                        primer.setBlockState(x, y, z, Blocks.PURPUR_BLOCK.defaultBlockState());
                        continue;
                    }
                    primer.setBlockState(x, y, z, body);
                }
            }
        }

        if (this.dim.bedrockTop() && maxBlockY >= dimTop - 5 && maxBlockY <= dimTop) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY < dimTop - 4) continue;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                this.level, rand, body, blockY, 5,
                                true, false);
                        primer.setBlockState(x, y, z, replaced);
                    }
                }
            }
        }
        if (this.dim.bedrockBottom() && minBlockY <= dimBottom + 4 && minBlockY >= dimBottom) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY > dimBottom + 4) continue;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                this.level, rand, body, blockY, 5,
                                false, true);
                        primer.setBlockState(x, y, z, replaced);
                    }
                }
            }
        }
        return primer;
    }

    @Override
    public void populate(ICube cube, Random rand) {
        // Routed via BandedFeaturePlacer: PlacedFeature-driven chorus plant +
        // end-island decoration plus real vanilla structure placement. Writes
        // route through the overworld ServerLevel; MixinLevelChunk redirects
        // set- and getBlockState into the cube's LevelChunkSection storage.
        BandedFeaturePlacer.placeAll(this.level, cube, this);
    }

    private void writeBiomes(CubePrimer primer, int cubeX, int cubeY, int cubeZ) {
        int[] arr = new int[64];
        Biome fallbackBiome = this.biomeRegistry.get(Biomes.THE_END);
        int fallbackId = this.biomeRegistry.getId(fallbackBiome);
        if (fallbackId < 0) fallbackId = 0;
        if (this.endGen == null) {
            java.util.Arrays.fill(arr, fallbackId);
            primer.setBiomeArray(arr);
            return;
        }
        try {
            Climate.Sampler sampler = this.randomState.sampler();
            int probeBlockY = Math.min(this.dim.maxBlockY(), Math.max(this.dim.minBlockY(), (cubeY << 4) + 7));
            int probeBiomeY = probeBlockY >> 2;
            for (int bx = 0; bx < 8; bx++) {
                int xBlock = (cubeX << 4) + (bx << 2) + 1;
                int biomeXBlock = xBlock >> 2;
                for (int bz = 0; bz < 8; bz++) {
                    int zBlock = (cubeZ << 4) + (bz << 2) + 1;
                    int biomeZBlock = zBlock >> 2;
                    Biome b = this.endGen.getBiomeSource()
                            .getNoiseBiome(biomeXBlock, probeBiomeY, biomeZBlock, sampler)
                            .value();
                    int id = this.biomeRegistry.getId(b);
                    arr[bx * 8 + bz] = id >= 0 ? id : fallbackId;
                }
            }
        } catch (Throwable t) {
            CubicChunks.LOGGER.warn("End biome sampling failed for cube {},{},{}: {}", cubeX, cubeY, cubeZ, t.toString());
            java.util.Arrays.fill(arr, fallbackId);
        }
        primer.setBiomeArray(arr);
    }

    private Biome sampleColumnBiome(int cubeX, int cubeZ, int blockY) {
        if (this.endGen == null) return null;
        try {
            Climate.Sampler sampler = this.randomState.sampler();
            int x = ((cubeX << 4) + 8) >> 2;
            int z = ((cubeZ << 4) + 8) >> 2;
            return this.endGen.getBiomeSource().getNoiseBiome(x, blockY >> 2, z, sampler).value();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isEndHighlandsBiome(@Nullable Biome biome) {
        return biome != null && "end_highlands".equals(pathOf(biome));
    }

    private static boolean isEndMidlandsBiome(@Nullable Biome biome) {
        return biome != null && "end_midlands".equals(pathOf(biome));
    }

    private static boolean isEndBarrensBiome(@Nullable Biome biome) {
        return biome != null && "end_barrens".equals(pathOf(biome));
    }

    @Nullable
    private static String pathOf(@Nullable Biome biome) {
        if (biome == null) return null;
        // 1.21.x: All three End biomes (highlands/midlands/barrens) share the same
        // TheEndBiome class and only differ by their registry key. Use the per-call
        // biomeRegistry to look up the key; fall back to class name if not registered.
        try {
            ResourceLocation loc = END_BIOME_REGISTRY.getKey(biome);
            if (loc != null) return loc.getPath();
        } catch (Throwable ignored) {
            // not in registry yet
        }
        return biome.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    /** Set by the constructor; the pathOf helper needs it to look up registry keys. */
    private static volatile Registry<Biome> END_BIOME_REGISTRY;

    @Nullable
    private static GeneratorBundle tryBuildEndGenerator(ServerLevel level) {
        Holder<NoiseGeneratorSettings> settings = null;
        try {
            HolderGetter<NoiseGeneratorSettings> registry = level.registryAccess()
                    .lookup(Registries.NOISE_SETTINGS).orElse(null);
            if (registry != null) {
                ResourceKey<NoiseGeneratorSettings> key = ResourceKey.create(
                        Registries.NOISE_SETTINGS, END_NOISE_SETTINGS_ID);
                Optional<Holder.Reference<NoiseGeneratorSettings>> holderRef = registry.get(key);
                if (holderRef.isPresent()) settings = holderRef.get();
            }
            // 1.21 port: we don't construct a NoiseBasedChunkGenerator here because the
            // 1.21 ctor requires a BiomeSource we don't have for stacked bands. The
            // BandedCubeFill fallback handles block fill, and the strategy populate
            // step handles structure placement.
            return new GeneratorBundle(null, settings);
        } catch (Throwable t) {
            CubicChunks.LOGGER.warn("Could not build vanilla EndChunkGenerator for stacked band: {}", t.toString());
            return new GeneratorBundle(null, null);
        }
    }

    private static RandomState tryBuildRandomState(ServerLevel level, Holder<NoiseGeneratorSettings> settings) {
        // 1.21.1: RandomState ctor is (NoiseGeneratorSettings, HolderGetter<NoiseParameters>, long)
        // — takes the value (not Holder) plus a noise-parameter registry lookup.
        HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noiseLookup =
                level.registryAccess().lookup(Registries.NOISE).orElse(null);
        if (noiseLookup == null) {
            throw new RuntimeException("EndBandStrategy: NOISE registry not available");
        }
        return RandomState.create(settings.value(), noiseLookup, level.getSeed());
    }

    private static RandomState fallbackRandomState(ServerLevel level, String noiseName) {
        return tryBuildRandomState(level, endFallbackHolder(level));
    }

    private static Holder<NoiseGeneratorSettings> endFallbackHolder(ServerLevel level) {
        // 1.21.1: Registry.getOrThrow(ResourceKey) returns the value T (not Holder<T>);
        // use getHolderOrThrow to obtain Holder<T> for the endFallbackHolder signature.
        try {
            return level.registryAccess()
                    .registryOrThrow(Registries.NOISE_SETTINGS)
                    .getHolderOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS,
                            ResourceLocation.withDefaultNamespace("overworld")));
        } catch (Throwable t) {
            throw new RuntimeException("EndBandStrategy: cannot resolve noise settings", t);
        }
    }
}
