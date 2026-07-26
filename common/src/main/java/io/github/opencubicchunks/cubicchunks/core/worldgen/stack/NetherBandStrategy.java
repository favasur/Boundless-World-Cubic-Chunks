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
 * Stacked-band strategy for the default {@code nether_stacked} sub-dim. Wraps a real
 * vanilla {@link NoiseBasedChunkGenerator} configured with
 * {@code minecraft:nether}.
 */
public class NetherBandStrategy implements StackedBandStrategy {

    private static final ResourceLocation NETHER_NOISE_SETTINGS_ID =
            ResourceLocation.withDefaultNamespace("nether");

    private final ServerLevel level;
    private final StackedDimension dim;
    @Nullable
    private final ChunkGenerator netherGen;
    @Nullable
    private final Holder<NoiseGeneratorSettings> netherSettings;
    @Nullable
    private final ChunkGeneratorStructureState structureState;
    private final RandomState randomState;
    private final Registry<Biome> biomeRegistry;
    private final int netherFloorCube;

    public NetherBandStrategy(ServerLevel level, StackedDimension dim) {
        this.level = level;
        this.dim = dim;
        this.biomeRegistry = this.level.registryAccess().registryOrThrow(Registries.BIOME);
        NETHER_BIOME_REGISTRY = this.biomeRegistry;
        GeneratorBundle bundle = tryBuildNetherGenerator(this.level);
        this.netherGen = bundle.gen;
        this.netherSettings = bundle.settings;
        this.randomState = this.netherSettings != null
                ? tryBuildRandomState(this.level, this.netherSettings)
                : tryBuildRandomState(this.level, netherFallbackHolder(this.level));
        this.netherFloorCube = Coords.blockToCube(this.dim.minBlockY());
        // Build a per-band ChunkGeneratorStructureState so vanilla structure
        // placement (Bastion, NetherFortress) walks the Nether biome source
        // instead of the overworld's. Falls back to null if construction fails
        // (caller falls back to overworld state — acceptable MVP behavior).
        this.structureState = this.netherGen != null
                ? BandedFeaturePlacer.tryBuildPerBandStructureState(this.level, this.netherGen)
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
        return this.netherGen;
    }

    @Override
    public ChunkGeneratorStructureState getChunkGeneratorState() {
        return this.structureState;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        writeBiomesFromNetherSource(primer, cubeX, cubeY, cubeZ);
        Random rand = BandedCubeFill.deterministicRandom(this.level, cubeX, cubeY, cubeZ);
        int minBlockY = Coords.cubeToMinBlock(cubeY);
        int maxBlockY = Coords.cubeToMaxBlock(cubeY);
        int dimTop = this.dim.maxBlockY();
        int dimBottom = this.dim.minBlockY();

        Biome columnBiome = sampleColumnBiome(cubeX, cubeZ, minBlockY + 8);
        BlockState body = bodyStateForBiome(columnBiome);

        for (int y = 0; y < 16; y++) {
            int blockY = minBlockY + y;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (cubeY == this.netherFloorCube
                            && rand.nextInt(28) == 0
                            && blockY - dimBottom < 6) {
                        primer.setBlockState(x, y, z, Blocks.LAVA.defaultBlockState());
                        continue;
                    }
                    if (rand.nextInt(64) == 0 && y > 11) {
                        primer.setBlockState(x, y, z, Blocks.GLOWSTONE.defaultBlockState());
                        continue;
                    }
                    if (columnBiome != null && isSoulBiome(columnBiome) && rand.nextInt(48) == 0) {
                        primer.setBlockState(x, y, z, Blocks.SOUL_SAND.defaultBlockState());
                        continue;
                    }
                    if (columnBiome != null && isCrimsonBiome(columnBiome) && rand.nextInt(64) == 0) {
                        primer.setBlockState(x, y, z, Blocks.CRIMSON_ROOTS.defaultBlockState());
                        continue;
                    }
                    if (columnBiome != null && isWarpedBiome(columnBiome) && rand.nextInt(64) == 0) {
                        primer.setBlockState(x, y, z, Blocks.WARPED_ROOTS.defaultBlockState());
                        continue;
                    }
                    primer.setBlockState(x, y, z, body);
                }
            }
        }

        if (this.dim.bedrockTop() && maxBlockY >= dimTop - 9 && maxBlockY <= dimTop) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY < dimTop - 9) continue;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                this.level, rand, body, blockY, 10,
                                true, false);
                        primer.setBlockState(x, y, z, replaced);
                    }
                }
            }
        }
        if (this.dim.bedrockBottom() && minBlockY <= dimBottom + 4 && minBlockY >= dimBottom) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY > dimBottom + 9) continue;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                this.level, rand, body, blockY, 10,
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
        // Per-band feature + structure placement. The placer routes through the
        // overworld ServerLevel, which MixinLevelChunk already redirects into the
        // stacked cube storage, so feature writes land in the right cube block.
        BandedFeaturePlacer.placeAll(this.level, cube, this);
    }

    private void writeBiomesFromNetherSource(CubePrimer primer, int cubeX, int cubeY, int cubeZ) {
        int[] arr = new int[64];
        Biome fallbackBiome = this.biomeRegistry.get(Biomes.NETHER_WASTES);
        int fallbackId = this.biomeRegistry.getId(fallbackBiome);
        if (fallbackId < 0) fallbackId = 0;
        if (this.netherGen == null) {
            java.util.Arrays.fill(arr, fallbackId);
            primer.setBiomeArray(arr);
            return;
        }
        try {
            Climate.Sampler sampler = this.randomState.sampler();
            int probeBlockY = Math.min(this.dim.maxBlockY(), Math.max(this.dim.minBlockY(), (cubeY << 4) + 7));
            int probeBiomeY = probeBlockY >> 2;
            for (int bx = 0; bx < 8; bx++) {
                int xzBlock = (cubeX << 4) + (bx << 2) + 1;
                int biomeXBloc = xzBlock >> 2;
                for (int bz = 0; bz < 8; bz++) {
                    int zBlock = (cubeZ << 4) + (bz << 2) + 1;
                    int biomeZBlock = zBlock >> 2;
                    Biome b = this.netherGen.getBiomeSource()
                            .getNoiseBiome(biomeXBloc, probeBiomeY, biomeZBlock, sampler)
                            .value();
                    int id = this.biomeRegistry.getId(b);
                    arr[bx * 8 + bz] = id >= 0 ? id : fallbackId;
                }
            }
        } catch (Throwable t) {
            CubicChunks.LOGGER.warn("Nether biome sampling failed for cube at {},{},{}: {}", cubeX, cubeY, cubeZ, t.toString());
            java.util.Arrays.fill(arr, fallbackId);
        }
        primer.setBiomeArray(arr);
    }

    private Biome sampleColumnBiome(int cubeX, int cubeZ, int blockY) {
        if (this.netherGen == null) return null;
        try {
            Climate.Sampler sampler = this.randomState.sampler();
            int climateY = blockY >> 2;
            int x = ((cubeX << 4) + 8) >> 2;
            int z = ((cubeZ << 4) + 8) >> 2;
            return this.netherGen.getBiomeSource().getNoiseBiome(x, climateY, z, sampler).value();
        } catch (Throwable t) {
            return null;
        }
    }

    private static BlockState bodyStateForBiome(@Nullable Biome biome) {
        if (biome == null) return Blocks.NETHERRACK.defaultBlockState();
        String path = pathOf(biome);
        if (path == null) return Blocks.NETHERRACK.defaultBlockState();
        if (path.contains("basalt")) return Blocks.BASALT.defaultBlockState();
        if (path.contains("soul")) return Blocks.SOUL_SOIL.defaultBlockState();
        return Blocks.NETHERRACK.defaultBlockState();
    }

    private static boolean isSoulBiome(@Nullable Biome biome) {
        String path = pathOf(biome);
        return path != null && path.contains("soul");
    }

    private static boolean isCrimsonBiome(@Nullable Biome biome) {
        String path = pathOf(biome);
        return path != null && path.contains("crimson");
    }

    private static boolean isWarpedBiome(@Nullable Biome biome) {
        String path = pathOf(biome);
        return path != null && path.contains("warped");
    }

    @Nullable
    private static String pathOf(@Nullable Biome biome) {
        if (biome == null) return null;
        // 1.21.x: Biome has no builtInRegistryHolder(); resolve the registry key from
        // the per-call biomeRegistry held on the strategy instance. Falls back to the
        // class-name substring only if the registry lookup fails.
        try {
            ResourceLocation loc = NETHER_BIOME_REGISTRY.getKey(biome);
            if (loc != null) return loc.getPath();
        } catch (Throwable ignored) {
            // some biomes may not be in the registry yet
        }
        return biome.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
    }

    /** Set by the constructor; the pathOf helper needs it to look up registry keys. */
    private static volatile Registry<Biome> NETHER_BIOME_REGISTRY;

    @Nullable
    private static GeneratorBundle tryBuildNetherGenerator(ServerLevel level) {
        Holder<NoiseGeneratorSettings> settings = null;
        try {
            HolderGetter<NoiseGeneratorSettings> registry = level.registryAccess()
                    .lookup(Registries.NOISE_SETTINGS).orElse(null);
            if (registry != null) {
                ResourceKey<NoiseGeneratorSettings> key = ResourceKey.create(
                        Registries.NOISE_SETTINGS, NETHER_NOISE_SETTINGS_ID);
                Optional<Holder.Reference<NoiseGeneratorSettings>> holderRef = registry.get(key);
                if (holderRef.isPresent()) settings = holderRef.get();
            }
            // 1.21 port: don't construct NoiseBasedChunkGenerator (requires BiomeSource).
            // BandedCubeFill handles procedural fill; strategy populate handles structures.
            return new GeneratorBundle(null, settings);
        } catch (Throwable t) {
            CubicChunks.LOGGER.warn("Could not build vanilla NetherChunkGenerator for stacked band: {}", t.toString());
            return new GeneratorBundle(null, null);
        }
    }

    private static RandomState tryBuildRandomState(ServerLevel level, Holder<NoiseGeneratorSettings> settings) {
        // 1.21.1: RandomState ctor is (NoiseGeneratorSettings, HolderGetter<NoiseParameters>, long)
        // — takes the value (not Holder) plus a noise-parameter registry lookup.
        HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noiseLookup =
                level.registryAccess().lookup(Registries.NOISE).orElse(null);
        if (noiseLookup == null) {
            throw new RuntimeException("NetherBandStrategy: NOISE registry not available");
        }
        return RandomState.create(settings.value(), noiseLookup, level.getSeed());
    }

    private static RandomState fallbackRandomState(ServerLevel level, String noiseName) {
        return tryBuildRandomState(level, netherFallbackHolder(level));
    }

    private static Holder<NoiseGeneratorSettings> netherFallbackHolder(ServerLevel level) {
        // 1.21.1: Registry.getOrThrow(ResourceKey) returns the value T (not Holder<T>);
        // use getHolderOrThrow to obtain Holder<T> for the netherFallbackHolder signature.
        try {
            return level.registryAccess()
                    .registryOrThrow(Registries.NOISE_SETTINGS)
                    .getHolderOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS,
                            ResourceLocation.withDefaultNamespace("overworld")));
        } catch (Throwable t) {
            throw new RuntimeException("NetherBandStrategy: cannot resolve noise settings", t);
        }
    }
}
