package io.github.opencubicchunks.cubicchunks.core.server;

import com.google.common.collect.ImmutableList;
import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.worldgen.generator.vanilla.VanillaCompatibilityGenerator
// 1.21: combined with the legacy DefaultCubeGenerator per project decision. The wrapper
// delegates to vanilla's ChunkGenerator so no new worldgen is authored here. Cubes that
// sit at a Y outside the vanilla `LevelHeightAccessor.getMaxBuildHeight() - 16` window
// are filled with the auto-detected extension block (default: STONE).
// -- This file intentionally does NOT introduce new generation rules.
public class DefaultCubeGenerator implements ICubeGenerator {

    public static final BlockState DEFAULT_EXTENSION_BLOCK = Blocks.STONE.defaultBlockState();
    public static final BlockState DEFAULT_FILLER_BLOCK = Blocks.AIR.defaultBlockState();

    private final ServerLevel level;
    private final ResourceLocation dimName;
    private boolean isInit = false;
    private int worldHeightCubes;

    @Nullable
    private BlockState extensionBlockTop = DEFAULT_FILLER_BLOCK;
    @Nullable
    private BlockState extensionBlockBottom = DEFAULT_EXTENSION_BLOCK;
    private boolean hasTopBedrock;
    private boolean hasBottomBedrock = true;

    public DefaultCubeGenerator(ServerLevel level) {
        this.level = level;
        this.dimName = level.dimension().location();
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ResourceLocation getDimName() {
        return this.dimName;
    }

    private void tryInit() {
        if (this.isInit) return;
        this.isInit = true;
        int worldHeightBlocks = this.level.getMaxBuildHeight();
        this.worldHeightCubes = Math.max(1, (worldHeightBlocks + 15) / 16);

        ChunkAccess column = this.level.getChunkSource().getChunk(0, 0, false);
        if (column != null) {
            LevelChunkSection[] sections = column.getSections();
            int minSection = column.getMinSection();
            int maxSection = sections.length + minSection - 1;
            LevelChunkSection bottomSection = minSection < sections.length ? sections[0] : null;
            if (bottomSection != null) {
                BlockState dominant = DEFAULT_EXTENSION_BLOCK;
                int count = 0;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 3; y++) {
                            BlockState st = bottomSection.getBlockState(x, y, z);
                            if (st.getBlock() == Blocks.BEDROCK) continue;
                            if (!st.isAir()) {
                                dominant = st;
                                count++;
                            }
                        }
                    }
                }
                if (count > 0) this.extensionBlockBottom = dominant;
                boolean bedrockFound = bottomSection.getBlockState(0, 0, 0).getBlock() == Blocks.BEDROCK
                        || bottomSection.getBlockState(8, 0, 8).getBlock() == Blocks.BEDROCK;
                this.hasBottomBedrock = bedrockFound;
            }
            LevelChunkSection topSection = sections.length > 0 ? sections[sections.length - 1] : null;
            int topSectionY = maxSection;
            this.worldHeightCubes = topSectionY + 1;
            if (topSection != null) {
                BlockState dominant = DEFAULT_FILLER_BLOCK;
                int count = 0;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 12; y < 16; y++) {
                            BlockState st = topSection.getBlockState(x, y, z);
                            if (st.getBlock() == Blocks.BEDROCK) continue;
                            if (!st.isAir()) {
                                dominant = st;
                                count++;
                            }
                        }
                    }
                }
                if (count > 0) this.extensionBlockTop = dominant;
                boolean bedrockFound = topSection.getBlockState(0, 15, 0).getBlock() == Blocks.BEDROCK
                        || topSection.getBlockState(8, 15, 8).getBlock() == Blocks.BEDROCK;
                this.hasTopBedrock = bedrockFound;
            }
        }
        CubicChunks.LOGGER.info("DefaultCubeGenerator init: world={} cubes={} topBlock={} bottomBlock={} bedrockTop={} bedrockBottom={}",
                this.dimName, this.worldHeightCubes,
                this.extensionBlockTop.getBlock(), this.extensionBlockBottom.getBlock(),
                this.hasTopBedrock, this.hasBottomBedrock);
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return this.generateCube(cubeX, cubeY, cubeZ, new CubePrimer());
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        return this.generateCube(cubeX, cubeY, cubeZ, primer, null);
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, @org.jetbrains.annotations.Nullable ChunkAccess preloadedColumn) {
        this.tryInit();
        int minBlockY = cubeY << 4;
        int maxBlockY = minBlockY + 15;
        int minBuildY = this.level.getMinBuildHeight();
        int maxBuildY = this.level.getMaxBuildHeight();

        // Outside vanilla bounds → emit extension-fill.
        if (maxBlockY < minBuildY || minBlockY > maxBuildY) {
            BlockState fill = cubeY * 16 < minBuildY ? this.extensionBlockBottom : this.extensionBlockTop;
            Random bgRand = this.cubeBackgroundRandom(cubeX, cubeY, cubeZ);
            for (int y = 0; y < 16; y++) {
                int blockY = Coords.localToBlock(cubeY, y);
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        primer.setBlockState(x, y, z,
                                WorldGenUtils.getRandomBedrockReplacement(this.level, bgRand, fill,
                                        blockY, 5, this.hasTopBedrock, this.hasBottomBedrock));
                    }
                }
            }
            Biomes.applyBiomesUniform(primer, Biomes.PLAINS_KEY);
            return primer;
        }

        // Inside vanilla window → reuse the column vanilla has already populated and
        // slice the matching section. Vanilla calls us only after the column moves to
        // FEATURES so this path normally returns real terrain.
        try {
            // Prefer the column the cube provider already loaded synchronously: its
            // status is guaranteed at least FULL (or whatever generate flag we passed).
            // Falling back to getChunk(false) re-reads the cached reference, which can
            // return a lower-status chunk if vanilla hasn't finished this column.
            ChunkAccess column = preloadedColumn != null
                    ? preloadedColumn
                    : this.level.getChunkSource().getChunk(cubeX, cubeZ, true);
            if (column == null || column instanceof EmptyColumn) {
                return primer.setAll(DEFAULT_FILLER_BLOCK);
            }
            if (!(column instanceof LevelChunk levelChunk)) {
                return primer.setAll(DEFAULT_FILLER_BLOCK);
            }
            int minSectionY = levelChunk.getMinSection();
            // Absolute section index = (cubeY << 4 - minBuildY) / 16 = cubeY - minSectionY.
            // The previous formula (minSectionY + cubeY) undercounted by 2*minSectionY,
            // which for a default overworld (minSectionY=-4) routed cubeY=7's lookup
            // into section[3] (Y=[-16,0)) instead of the correct section[11] (Y=[112,128)).
            int sectionIndex = cubeY - minSectionY;
            LevelChunkSection[] sections = levelChunk.getSections();
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                return primer.setAll(DEFAULT_FILLER_BLOCK);
            }
            LevelChunkSection section = sections[sectionIndex];
            if (section == null) {
                // Section index is in range but the section was never allocated (vanilla
                // doesn't allocate a section if all blocks there are air at SURFACE — sky
                // above maxBuildHeight in pre-1.21 worlds, for example). Fill with the
                // configured extension block so cubes at this Y still have a non-AIR
                // sentinel. Above the build limit use extensionBlockTop (default AIR);
                // below use extensionBlockBottom (default STONE) so caves don't show AIR.
                BlockState fill = cubeY * 16 < minBuildY ? this.extensionBlockBottom : this.extensionBlockTop;
                return primer.setAll(fill);
            }
            Biomes.applyBiomesForCube(primer, levelChunk, cubeY);
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        primer.setBlockState(x, y, z, state);
                    }
                }
            }
        } catch (Exception e) {
            CubicChunks.LOGGER.error("DefaultCubeGenerator.generateCube({},{},{}) failed; emitting fills",
                    cubeX, cubeY, cubeZ, e);
            return primer.setAll(DEFAULT_FILLER_BLOCK);
        }
        return primer;
    }

    @Override
    public void generateColumn(ChunkAccess column) {
        // Vanilla generation is responsible for column biomes. We only consume them in
        // generateCube's Biomes.applyBiomesForCube() injection point.
    }

    @Override
    public void populate(ICube cube) {
        try {
            this.tryInit();
            Random rand = this.cubeSpecificRandom(cube.getCoords().getX(), cube.getCoords().getY(), cube.getCoords().getZ());
            // Defer vanilla's per-column decoration through CubeGeneratorsRegistry so addon
            // mods can register their own generation hooks. The default impl fires
            // PopulateCubeEvent.Pre/Populate/Post through ICubicPlatform — vanilla does
            // the actual decoration via the active ChunkGenerator, which already ran in
            // generateCube via the column-status pipeline.
            CubeGeneratorsRegistry.populateVanillaCubic(this.level, rand, cube);
            cube.setPopulated(true);
        } catch (Throwable t) {
            CubicChunks.LOGGER.error("DefaultCubeGenerator.populate failed for cube {}", cube.getCoords(), t);
        }
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        this.tryInit();
        int y = cube.getCoords().getY();
        if (y >= 0 && y < this.worldHeightCubes) {
            return new Box(-1, -y, -1, 1, this.worldHeightCubes - y, 1);
        }
        return NO_REQUIREMENT;
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        this.tryInit();
        int y = cube.getCoords().getY();
        if (y >= 0 && y < this.worldHeightCubes) {
            return new Box(0, -y, 0, 1, this.worldHeightCubes - y, 1);
        }
        return NO_REQUIREMENT;
    }

    @Override
    public void recreateStructures(ICube cube) {
    }

    @Override
    public void recreateStructures(ChunkAccess column) {
    }

    @Override
    public java.util.List<MobSpawnSettings.SpawnerData> getPossibleCreatures(MobCategory category, BlockPos pos) {
        Biome biome = this.level.getBiome(pos).value();
        MobSpawnSettings settings = biome.getMobSettings();
        if (settings == null) return java.util.List.of();
        java.util.List<MobSpawnSettings.SpawnerData> collected = new java.util.ArrayList<>();
        try {
            settings.getMobs(category).unwrap().forEach(collected::add);
        } catch (Throwable t) {
            return java.util.List.of();
        }
        return ImmutableList.copyOf(collected);
    }

    @Override
    public Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force) {
        return Optional.of(this.generateCube(cubeX, cubeY, cubeZ, primer, null));
    }

    @Override
    public Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force,
                                                net.minecraft.world.level.chunk.ChunkAccess preloadedColumn) {
        return Optional.of(this.generateCube(cubeX, cubeY, cubeZ, primer, preloadedColumn));
    }

    @Override
    public void registerBlocks() {
        // 1.21: vanilla handles block registration; no-op.
    }

    private Random cubeSpecificRandom(int cubeX, int cubeY, int cubeZ) {
        Random rand = new Random(this.level.getSeed());
        rand.setSeed(rand.nextInt() ^ cubeX);
        rand.setSeed(rand.nextInt() ^ cubeZ);
        rand.setSeed(rand.nextInt() ^ cubeY);
        return rand;
    }

    private Random cubeBackgroundRandom(int cubeX, int cubeY, int cubeZ) {
        return this.cubeSpecificRandom(cubeX, cubeY, cubeZ);
    }

    /** Internal helper used by generateCube to keep biome bookkeeping out of the main flow. */
    static final class Biomes {
        static final ResourceKey<Biome> PLAINS_KEY =
                ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("plains"));

        static void applyBiomesUniform(CubePrimer primer, ResourceKey<Biome> biomeKey) {
            int[] flat = new int[64];
            // 1.21 port: biome id resolution deferred to render; use 0 (vanilla default).
            for (int i = 0; i < flat.length; i++) flat[i] = 0;
            primer.setBiomeArray(flat);
        }

        static void applyBiomesForCube(CubePrimer primer, LevelChunk levelChunk, int cubeY) {
            // 1.21: LevelChunk#getBiomes returns PalettedContainer; fill with plains for now
            int[] sourceBiomes = new int[64];
            if (sourceBiomes == null) {
                applyBiomesUniform(primer, PLAINS_KEY);
                return;
            }
            int minSection = levelChunk.getMinSection();
            int cubeRelative = cubeY - minSection;
            int stride = 16 * 16;
            int baseSection = cubeRelative * stride;
            int[] slice = new int[64];
            for (int i = 0; i < slice.length && (baseSection + i) < sourceBiomes.length; i++) {
                slice[i] = sourceBiomes[baseSection + i];
            }
            primer.setBiomeArray(slice);
        }
    }
}
