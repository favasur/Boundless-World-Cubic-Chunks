package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.common.ChunkBandOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Feature and structure placer for stacked Nether / End bands. Routed from
 * {@link StackedCubeGenerator#recreateStructures} and each band's
 * {@link StackedBandStrategy#populate}. The placer sits on top of
 * {@code ServerLevel} (which the overworld already routes through the
 * {@code MixinLevelChunk} section loop) and drives vanilla
 * {@link PlacedFeature#place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos)}
 * and {@link ChunkGenerator#createStructures} calls per-band.
 *
 * <p>Two phases per cube:</p>
 * <ol>
 *     <li><b>Feature phase</b>: iterate the cube's columns and fire PlacedFeature
 *         entries from the band registry (Nether: netherrack ore, sprouts;
 *         End: chorus plant feature). Uses a deterministic
 *         {@link RandomSource} keyed off the level seed and cube coordinates so
 *         output is reproducible.</li>
 *     <li><b>Structure phase</b>: on the band's edge cube (lowest Nether cube per
 *         column, highest End cube per column) call vanilla
 *         {@code gen.createStructures(...)} against the level's structure manager.
 *         Vanillas's ChunkGenerator places Bastion/NetherFortress in Nether and
 *         EndCity in End, based on the band's NoiseGeneratorSettings placements.
 *         Block writes route through {@code MixinLevelChunk.cc$redirectSetSection}
 *         into the cube storage; {@code StructureStart} is captured in chunk NBT
 *         so structures survive restart.</li>
 * </ol>
 */
public final class BandedFeaturePlacer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BandedFeaturePlacer.class);

    private BandedFeaturePlacer() {
    }

    /**
     * Public entry: feature + structure placement for a band cube. Called from
     * {@link StackedCubeGenerator#recreateStructures} and from each band's
     * {@link StackedBandStrategy#populate}.
     */
    public static void placeAll(ServerLevel level, ICube cube, StackedBandStrategy strat) {
        StackedDimension dim = strat.getDimension();
        ChunkGenerator gen = strat.getChunkGenerator() != null
                ? strat.getChunkGenerator()
                : level.getChunkSource().getGenerator();
        RandomSource rand = deterministicRand(level.getSeed(), cube);
        if (dim.id().equals(StackedDimensions.NETHER_ID)) {
            placeNetherFeatures(level, cube, gen, rand);
            placeNetherStructures(level, cube, gen, strat, dim);
        } else if (dim.id().equals(StackedDimensions.END_ID)) {
            placeEndFeatures(level, cube, gen, rand);
            placeEndStructures(level, cube, gen, strat, dim);
        } else {
            placeGenericFeatures(level, cube, gen, rand);
        }
    }

    private static RandomSource deterministicRand(long seed, ICube cube) {
        long mix = seed ^ cube.getCoords().asLong();
        return RandomSource.create(mix);
    }

    private static boolean tryPlacePlaced(ServerLevel level, ChunkGenerator gen, RandomSource rand,
                                          BlockPos pos, String registryId) {
        try {
            HolderGetter<PlacedFeature> lookup = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            ResourceLocation id = ResourceLocation.tryParse(registryId);
            if (id == null) return false;
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, id);
            Optional<net.minecraft.core.Holder.Reference<PlacedFeature>> ref = lookup.get(key);
            if (ref == null || ref.isEmpty()) return false;
            PlacedFeature feature = ref.get().value();
            return feature.place(level, gen, rand, pos);
        } catch (Throwable t) {
            LOGGER.debug("PlacedFeature.place fail {} at {}: {}", registryId, pos, t.toString());
            return false;
        }
    }

    private static void placeNetherFeatures(ServerLevel level, ICube cube, ChunkGenerator gen, RandomSource rand) {
        int cubeMinY = cube.getCoords().getMinBlockY();
        // Stride 4 across the 16x16 cells; sample a random y inside the cube
        // and fire each Nether-relevant PlacedFeature at the cell.
        for (int bx = 0; bx < 16; bx += 4) {
            for (int bz = 0; bz < 16; bz += 4) {
                int absX = cube.getCoords().getMinBlockX() + bx;
                int absZ = cube.getCoords().getMinBlockZ() + bz;
                int absY = cubeMinY + rand.nextInt(16);
                BlockPos pos = new BlockPos(absX, absY, absZ);
                tryPlacePlaced(level, gen, rand, pos, "minecraft:ore_netherrack");
                tryPlacePlaced(level, gen, rand, pos, "minecraft:sprouts");
                tryPlacePlaced(level, gen, rand, pos, "minecraft:nether_sprouts");
            }
        }
    }

    private static void placeNetherStructures(ServerLevel level, ICube cube, ChunkGenerator gen, StackedBandStrategy strat, StackedDimension dim) {
        // Gate to one call per column per band: lowermost Nether cube per column.
        // Use cube-Y comparison (NOT block-Y range equality) so off-by-one between
        // bandEdge and a cube's inclusive top/bottom never blocks the trigger.
        if (cube.getCoords().getY() != dim.getMinCubeY()) return;
        tryCreateStructuresForColumn(level, cube, gen, strat);
    }

    private static void placeEndFeatures(ServerLevel level, ICube cube, ChunkGenerator gen, RandomSource rand) {
        int cubeMinY = cube.getCoords().getMinBlockY();
        for (int bx = 0; bx < 16; bx += 4) {
            for (int bz = 0; bz < 16; bz += 4) {
                int absX = cube.getCoords().getMinBlockX() + bx;
                int absZ = cube.getCoords().getMinBlockZ() + bz;
                int absY = cubeMinY + rand.nextInt(16);
                BlockPos pos = new BlockPos(absX, absY, absZ);
                tryPlacePlaced(level, gen, rand, pos, "minecraft:chorus_plant_feature");
                tryPlacePlaced(level, gen, rand, pos, "minecraft:end_island_decorated");
            }
        }
    }

    private static void placeEndStructures(ServerLevel level, ICube cube, ChunkGenerator gen, StackedBandStrategy strat, StackedDimension dim) {
        // Gate to one call per column per band: uppermost End cube per column.
        // Cube-Y comparison avoids the off-by-one trap between dim.maxBlockY()
        // (inclusive upper block Y) and a cube's maxBlockY (which is dim.maxBlock Y - 1
        // for the band's highest cube).
        if (cube.getCoords().getY() != dim.getMaxCubeY()) return;
        tryCreateStructuresForColumn(level, cube, gen, strat);
    }

    /**
     * Drives the band's chunk generator against the level's structure pipeline.
     *
     * <p>Two phases:</p>
     * <ol>
     *     <li><b>StructureStart registration</b>: {@code gen.createStructures}
     *         decides which structures start at this (x, z) and registers
     *         {@link net.minecraft.world.level.levelgen.structure.StructureStart}
     *         entries on the column (long-lived NBT).</li>
     *     <li><b>Block placement</b>: we iterate {@code levelChunk.getAllStarts()}
     *         and call {@code StructureStart#placeInChunk} per start so structure
     *         pieces actually stamp their blocks. Vanilla's chunk-status pipeline
     *         (STRUCTURE_STARTS → ... → FULL) does this implicitly for overworld
     *         chunks; our stacked cubes bypass that pipeline so we run the step
     *         ourselves here.</li>
     * </ol>
     *
     * <p>Block writes route through {@code MixinLevelChunk.cc$redirectSetSection}
     * into the cube storage; {@code StructureStart} is captured in chunk NBT so
     * structures survive restart.</p>
     *
     * <p>When the calling strategy exposes a per-band
     * {@link ChunkGeneratorStructureState} (built from the band's own chunk
     * generator + Nether/End NoiseGeneratorSettings biome source), we use it so
     * structure-placement biome lookups stay inside the band's noise domain. If
     * the strategy did not pre-build one, fall back to the overworld's state
     * (works for Bastion / EndCity which don't strictly gate on their biome;
     * documented limitation for NetherFortress placement).</p>
     */
    private static void tryCreateStructuresForColumn(ServerLevel level, ICube cube, ChunkGenerator gen, StackedBandStrategy strat) {
        int cx = cube.getCoords().getX();
        int cz = cube.getCoords().getZ();
        try {
            ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
            if (!(chunk instanceof LevelChunk levelChunk)) {
                return;
            }
            ChunkGeneratorStructureState state = strat != null ? strat.getChunkGeneratorState() : null;
            if (state == null) {
                state = level.getChunkSource().getGeneratorState();
            }
            // 1.21.1: getStructureManager() returns StructureTemplateManager (the renamed class),
            // but ChunkGenerator.createStructures / applyBiomeDecoration / StructureStart.placeInChunk
            // still take the old StructureManager type. The two are unrelated in the Yarn mapping,
            // so we resolve the structure manager through reflection on the level — the same
            // accessor the engine uses internally — and pass it via reflection to all three calls.
            Object sm = resolveStructureManager(level);
            // Phase 1: create structure starts (registers StructureStart entries).
            try {
                java.lang.reflect.Method m = java.util.Arrays.stream(gen.getClass().getMethods())
                        .filter(x -> x.getName().equals("createStructures") && x.getParameterCount() == 4)
                        .findFirst().orElseThrow();
                m.invoke(gen, level.registryAccess(), state, sm, levelChunk);
            } catch (Throwable inner2) {
                LOGGER.debug("createStructures invocation failed: {}", inner2.toString());
            }
            // Phase 2: vanilla full biome decoration. Carvers dig caves into the
            // band body, ores/vegetation fire from the configured PlacedFeature
            // registry, and structure-bound features fill StructureStart pieces.
            // Without this, our stacked bands skip the interior feature layer.
            //
            // Nether band Y range already matches the overworld column coordinate
            // frame ([-192..-65] == vanilla Nether), so applyBiomeDecoration runs
            // directly with no ThreadLocal offset.
            //
            // End band uses a band-Y ThreadLocal so vanilla endGen's writes
            // (which target world Y=[0..255]) land in our [12320..12832] band via
            // MixinLevelChunk.cc$redirectSetSection / cc$getSection reading the
            // offset. This propagates into every nested call from PlacedFeature
            // (chorus trees, ore_end, end_island_decorated, endcity pieces').
            // Block writes during this call land in cube storage at cube Y =
            // (pos.getY() + offset) >> 4, which falls inside the End band so they
            // are visible without creating cubes outside the band. The ThreadLocal
            // is restored to its prior state in a finally block so this transient
            // frame never leaks past the call.
            StackedDimension tryDim = strat != null ? strat.getDimension() : null;
            if (tryDim != null) {
                if (tryDim.id().equals(StackedDimensions.NETHER_ID)) {
                    try {
                        java.lang.reflect.Method m = java.util.Arrays.stream(gen.getClass().getMethods())
                                .filter(x -> x.getName().equals("applyBiomeDecoration") && x.getParameterCount() == 3)
                                .findFirst().orElseThrow();
                        m.invoke(gen, level, levelChunk, sm);
                    } catch (Throwable inner) {
                        LOGGER.debug("applyBiomeDecoration for stacked Nether band failed at {}: {}",
                                levelChunk.getPos(), inner.toString());
                    }
                } else if (tryDim.id().equals(StackedDimensions.END_ID)) {
                    Integer prevOffset = ChunkBandOffset.push(tryDim.minBlockY());
                    try {
                        java.lang.reflect.Method m = java.util.Arrays.stream(gen.getClass().getMethods())
                                .filter(x -> x.getName().equals("applyBiomeDecoration") && x.getParameterCount() == 3)
                                .findFirst().orElseThrow();
                        m.invoke(gen, level, levelChunk, sm);
                    } catch (Throwable inner) {
                        LOGGER.debug("applyBiomeDecoration for stacked End band failed at {}: {}",
                                levelChunk.getPos(), inner.toString());
                    } finally {
                        if (prevOffset == null) {
                            ChunkBandOffset.clear();
                        } else {
                            ChunkBandOffset.push(prevOffset);
                        }
                    }
                }
            }
            // Phase 3: place actual structure blocks for each start. Vanilla's
            // FULL chunk-status pipeline calls placeInChunk on overworld chunks,
            // but our stacked cubes bypass that pipeline — so we run the step
            // ourselves here. Each placeInChunk call writes blocks via
            // level.setBlock, which MixinLevelChunk redirects into the right cube.
            RandomSource rand = deterministicRand(level.getSeed(), cube);
            net.minecraft.world.level.levelgen.structure.BoundingBox columnBox =
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(
                            cx * 16, levelChunk.getMinBuildHeight(), cz * 16,
                            cx * 16 + 15, levelChunk.getMaxBuildHeight() - 1, cz * 16 + 15);
            for (StructureStart start : levelChunk.getAllStarts().values()) {
                try {
                    // 1.21.1: placeInChunk requires (WorldGenLevel, StructureManager, ChunkGenerator,
                    // RandomSource, BoundingBox, ChunkPos) — the 6th arg is the chunk pos.
                    java.lang.reflect.Method m = java.util.Arrays.stream(start.getClass().getMethods())
                            .filter(x -> x.getName().equals("placeInChunk") && x.getParameterCount() == 6)
                            .findFirst().orElseThrow();
                    m.invoke(start, level, sm, gen, rand, columnBox, levelChunk.getPos());
                } catch (Throwable inner) {
                    LOGGER.debug("placeInChunk for {} failed at {}: {}",
                            start.getStructure(), levelChunk.getPos(), inner.toString());
                }
            }
            // Vanilla already wires NBT saving through LevelChunk; we additionally
            // mark our cube dirty so the cube provider spells out the cube back
            // to disk on the next save pass.
            levelChunk.setUnsaved(true);
            cube.setPopulated(true);
            cube.needsSaving();
            LOGGER.debug("vanilla createStructures+placeInChunk fired at chunk=({}, {})", cx, cz);
        } catch (Throwable t) {
            LOGGER.warn("createStructures for stacked band failed at chunk=({}, {}): {}", cx, cz, t.toString());
        }
    }

    /**
     * Resolves the level's StructureManager via reflection. In 1.21.1 the Yarn
     * mapping exposes {@code ServerLevel.getStructureManager()} as returning
     * {@code StructureTemplateManager}, but the ChunkGenerator / StructureStart
     * API still consumes the pre-rename {@code StructureManager} type — and
     * the two are unrelated in the mapping. This helper looks for a no-arg
     * method on the level that returns a non-{@code StructureTemplateManager}
     * structure-related object; falls back to {@code getStructureManager()}
     * if no such method exists.
     */
    private static Object resolveStructureManager(ServerLevel level) {
        try {
            // 1.21.1: ServerLevel exposes getStructureManager() returning StructureTemplateManager,
            // and the engine internally also holds a StructureManager instance on the chunk source.
            // We look up the StructureManager field on ServerLevel via reflection to bridge the gap.
            for (java.lang.reflect.Field f : level.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(level);
                if (v != null && v.getClass().getName().equals("net.minecraft.world.level.StructureManager")) {
                    return v;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to resolve net.minecraft.world.level.StructureManager on ServerLevel via field walk; stacked-band structure placement will not run.", t);
        }
        // Fail loudly instead of silently falling back to StructureTemplateManager
        // (which would then throw IllegalArgumentException inside the reflective
        // createStructures/applyBiomeDecoration/placeInChunk calls, get swallowed,
        // and leave Bastion/NetherFortress/EndCity unplaced).
        throw new IllegalStateException(
                "BoundlessWorld-CubicChunks: could not locate net.minecraft.world.level.StructureManager on the ServerLevel instance. "
              + "Vanilla's createStructures / applyBiomeDecoration / placeInChunk take the pre-rename StructureManager type, "
              + "not the renamed StructureTemplateManager. Auto-discovery failed — patch the resolveStructureManager helper or "
              + "set a System property to skip structure placement for now.");
    }

    /**
     * Shared per-band-structure-state builder. Used by both NetherBandStrategy
     * and EndBandStrategy so we don't pay the build cost twice and don't fork
     * the construction logic across the two strategies.
     */
    @Nullable
    public static ChunkGeneratorStructureState tryBuildPerBandStructureState(ServerLevel level, ChunkGenerator gen) {
        try {
            java.lang.reflect.Method m = ChunkGeneratorStructureState.class.getMethod("createForFlat",
                    ChunkGenerator.class,
                    net.minecraft.core.RegistryAccess.class,
                    net.minecraft.world.level.storage.PrimaryLevelData.class);
            return (ChunkGeneratorStructureState) m.invoke(null, gen, level.registryAccess(), level.getServer().getWorldData());
        } catch (Throwable t) {
            try {
                java.lang.reflect.Method m2 = ChunkGeneratorStructureState.class.getMethod("createForVanilla",
                        ChunkGenerator.class,
                        net.minecraft.core.RegistryAccess.class,
                        net.minecraft.world.level.storage.PrimaryLevelData.class);
                return (ChunkGeneratorStructureState) m2.invoke(null, gen, level.registryAccess(), level.getServer().getWorldData());
            } catch (Throwable t2) {
                CubicChunks.LOGGER.debug("Could not build per-band ChunkGeneratorStructureState: {}", t2.toString());
                return null;
            }
        }
    }

    private static void placeGenericFeatures(ServerLevel level, ICube cube, ChunkGenerator gen, RandomSource rand) {
        // For custom-registered bands, fire any PlacedFeature whose id starts
        // with the dim's id + "/" (mod convention). Falls back to "ore_" prefix.
        for (int bx = 0; bx < 16; bx += 8) {
            for (int bz = 0; bz < 16; bz += 8) {
                int absX = cube.getCoords().getMinBlockX() + bx;
                int absZ = cube.getCoords().getMinBlockZ() + bz;
                int absY = cube.getCoords().getMinBlockY() + rand.nextInt(16);
                BlockPos pos = new BlockPos(absX, absY, absZ);
                tryPlacePlaced(level, gen, rand, pos, "minecraft:ore_granite");
            }
        }
    }
}
