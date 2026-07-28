package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionPalette;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.DefaultCubeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICubeGenerator that wraps a per-dimension {@link DefaultCubeGenerator} (used for the
 * overworld Y window) with per-band {@link StackedBandStrategy} instances for stacked
 * sub-dims (Nether, End, and any custom registered sub-dim). Generation is dispatched
 * by cube Y: a cube whose Y is inside a {@link StackedDimension} record runs through
 * its band strategy; a cube whose Y falls through to the active vanilla window gets
 * the existing per-section column-slice path populated by {@link DefaultCubeGenerator}.
 *
 * <p>This is the A3 WRAPPER architectural decision surfaced through code: single cube
 * provider, single save file, but generation writes the right blocks per band. The
 * Nether and End strategies live in this package and resolve a real
 * {@link net.minecraft.world.level.chunk.ChunkGenerator} from the overworld's data
 * registries so {@code getNoiseBiome} samples the actual Nether / End biome presets.</p>
 *
 * <p>Mob spawn lists are dispatched by cubeY: bands override
 * {@code getPossibleCreatures(MobCategory, BlockPos)} so a {@code blaze} spawner query
 * in the Nether band returns the Nether spawn list rather than pigs.</p>
 */
// @Original: original mod used worldgen.MultiDimVerticalHopperChain. 1.21 rewrite as a
// wrap-then-fill ICubeGenerator that owns one DefaultCubeGenerator per stacked band.
public class StackedCubeGenerator implements ICubeGenerator {

    private static final int PROXIMITY_THRESHOLD_BLOCKS = 512;

    private final ServerLevel level;
    private final ResourceLocation dimName;
    private final DefaultCubeGenerator overworldGen;
    private final Map<ResourceLocation, BandCubeGenerator> bandGens = new HashMap<>();

    /**
     * Tracks which stacked bands have been activated (by portal use or player
     * proximity). A band stays active for the remainder of the session once
     * activated. Non-active bands behave as empty air — no cubes are generated
     * or saved to disk until the band is activated.
     */
    private final Set<ResourceLocation> activeBands = ConcurrentHashMap.newKeySet();

    public StackedCubeGenerator(ServerLevel level) {
        this.level = level;
        this.dimName = level.dimension().location();
        this.overworldGen = new DefaultCubeGenerator(level);
        rebuildBandGenerators();
    }

    public synchronized void rebuildBandGenerators() {
        this.bandGens.clear();
        for (StackedDimension dim : StackedDimensionRegistry.all()) {
            if (dim.id().equals(StackedDimensions.OVERWORLD_ID)) {
                continue;
            }
            this.bandGens.put(dim.id(), new BandCubeGenerator(this.level, dim));
        }
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ResourceLocation getDimName() {
        return this.dimName;
    }

    public Optional<StackedDimension> resolveStackedSubDim(int cubeY) {
        return StackedDimensionRegistry.findForCubeY(cubeY);
    }

    private boolean isInVanillaColumn(int cubeY) {
        int minSection = Coords.blockToCube(this.level.getMinBuildHeight());
        int maxSection = Coords.blockToCube(this.level.getMaxBuildHeight() - 1);
        return cubeY >= minSection && cubeY <= maxSection;
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return this.generateCube(cubeX, cubeY, cubeZ, new CubePrimer());
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        if (matched.isPresent() && !isInVanillaColumn(cubeY)) {
            StackedDimension dim = matched.get();
            // Lazy band activation: only generate cubes in this band if a player
            // has entered its portal or approached its Y level in the overworld.
            if (!activeBands.contains(dim.id()) && !dim.id().equals(StackedDimensions.OVERWORLD_ID)) {
                return primer; // treat as empty air until activated
            }
            BandCubeGenerator band = this.bandGens.get(dim.id());
            if (band != null) {
                return band.generateCube(cubeX, cubeY, cubeZ, primer);
            }
            return BandedCubeFill.fillBand(primer, dim, this.level, cubeX, cubeY, cubeZ);
        }
        // Empty-space rule: when cubeY is outside any registered stacked dim AND outside
        // the overworld's vanilla window, leave the primer as air so the Cube ctor's
        // "no blocks = no isModified" branch keeps the disk usage at zero for the
        // endless vertical buffer between bands and at the world's sky / bedrock caps.
        if (matched.isEmpty() && !isInVanillaColumn(cubeY)) {
            return primer;
        }
        return this.overworldGen.generateCube(cubeX, cubeY, cubeZ, primer);
    }

    @Override
    public Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean force) {
        return Optional.of(generateCube(cubeX, cubeY, cubeZ, primer));
    }

    @Override
    public void generateColumn(ChunkAccess column) {
    }

    @Override
    public void populate(ICube cube) {
        int cubeY = cube.getCoords().getY();
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        Random rand = BandedCubeFill.deterministicRandom(this.level,
                cube.getCoords().getX(), cube.getCoords().getY(), cube.getCoords().getZ());

        if (matched.isPresent() && !isInVanillaColumn(cubeY)) {
            CubeGeneratorsRegistry.populateVanillaCubic(this.level, rand, cube);
            BandCubeGenerator band = this.bandGens.get(matched.get().id());
            if (band != null) {
                band.populate(cube, rand);
            }
            cube.setPopulated(true);
            return;
        }
        this.overworldGen.populate(cube);
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        int cubeY = cube.getCoords().getY();
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        if (matched.isPresent() && !isInVanillaColumn(cubeY)) {
            return NO_REQUIREMENT;
        }
        return this.overworldGen.getFullPopulationRequirements(cube);
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        int cubeY = cube.getCoords().getY();
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        if (matched.isPresent() && !isInVanillaColumn(cubeY)) {
            return NO_REQUIREMENT;
        }
        return this.overworldGen.getPopulationPregenerationRequirements(cube);
    }

    @Override
    public void recreateStructures(ICube cube) {
        int cubeY = cube.getCoords().getY();
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        if (matched.isPresent() && !isInVanillaColumn(cubeY)) {
            BandCubeGenerator band = this.bandGens.get(matched.get().id());
            if (band != null) {
                // Re-run feature + structure placement for this cube in its band.
                BandedFeaturePlacer.placeAll(this.level, cube, band.strategy());
            }
        }
    }

    @Override
    public void recreateStructures(ChunkAccess column) {
        // Per-column recreation is unused in cubic stacking; the per-cube flow above
        // covers every cube the cube provider will load for this column.
    }

    /**
     * Mob spawn dispatch: when the spawn position sits in a stacked sub-dim band,
     * return that band's mob spawn list (Nether → blaze/piglin/etc., End → enderman/
     * shulker). Otherwise delegate to the overworld biome.
     */
    @Override
    public List<MobSpawnSettings.SpawnerData> getPossibleCreatures(MobCategory category, BlockPos pos) {
        int cubeY = Coords.blockToCube(pos.getY());
        Optional<StackedDimension> matched = resolveStackedSubDim(cubeY);
        if (matched.isEmpty() || isInVanillaColumn(cubeY)) {
            return this.overworldGen.getPossibleCreatures(category, pos);
        }
        return StackedMobSpawnLists.get().get(matched.get(), category);
    }

    /**
     * Inner helper that owns the per-band strategy dispatch. Each registered
     * non-overworld sub-dim has a strategy (real Nether / End chunk generator for the
     * built-in bands, or a {@link BandedCubeFillStrategy} fallback for custom bands).
     */
    public static final class BandCubeGenerator {
        private final ServerLevel level;
        private final StackedDimension dim;
        private final StackedBandStrategy strategy;

        BandCubeGenerator(ServerLevel level, StackedDimension dim) {
            this.level = level;
            this.dim = dim;
            this.strategy = selectStrategy(level, dim);
        }

        public StackedDimension getDim() {
            return this.dim;
        }

        public StackedDimensionPalette getPalette() {
            return this.dim.palette();
        }

        public ServerLevel getLevel() {
            return this.level;
        }

        public StackedBandStrategy strategy() {
            return this.strategy;
        }

        CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
            return this.strategy.generateCube(cubeX, cubeY, cubeZ, primer);
        }

        void populate(ICube cube, Random rand) {
            this.strategy.populate(cube, rand);
        }

        private static StackedBandStrategy selectStrategy(ServerLevel level, StackedDimension dim) {
            ResourceLocation id = dim.id();
            if (id.equals(StackedDimensions.NETHER_ID)) {
                return new NetherBandStrategy(level, dim);
            }
            if (id.equals(StackedDimensions.END_ID)) {
                return new EndBandStrategy(level, dim);
            }
            return new BandedCubeFillStrategy(level, dim);
        }
    }

    /**
     * Activates a stacked band so it starts generating cubes. Called from the
     * portal mixin when a player enters a Nether/End portal, or from
     * {@link #checkPlayerProximity()} when a player flies close to a band's Y level.
     * Once activated, a band stays active for the remainder of the server session.
     */
    public void activateBand(ResourceLocation bandId) {
        if (activeBands.add(bandId)) {
            CubicChunks.LOGGER.info("Stacked band {} activated (lazy generation triggered)", bandId);
        }
    }

    /**
     * Returns true if the given stacked band has been activated for cube generation.
     */
    public boolean isBandActive(ResourceLocation bandId) {
        return activeBands.contains(bandId);
    }

    /**
     * Scans all online players and activates any stacked band whose Y range is
     * within {@link #PROXIMITY_THRESHOLD_BLOCKS} blocks of a player. Called once
     * per tick from {@link io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer#tick}.
     */
    public void checkPlayerProximity() {
        for (ServerPlayer player : level.players()) {
            double playerY = player.getY();
            for (StackedDimension dim : StackedDimensionRegistry.all()) {
                if (dim.id().equals(StackedDimensions.OVERWORLD_ID)) continue;
                if (activeBands.contains(dim.id())) continue; // already active

                // Compute the shortest vertical distance from the player to the band.
                int distAbove = dim.minBlockY() - (int) playerY;
                int distBelow = (int) playerY - dim.maxBlockY();
                int dist = Math.max(distAbove, distBelow);

                if (dist <= PROXIMITY_THRESHOLD_BLOCKS) {
                    activateBand(dim.id());
                }
            }
        }
    }

    /**
     * Fallback strategy that simply delegates to the existing
     * {@link BandedCubeFill}. Used for any custom stacked sub-dim that doesn't have
     * its own real chunk generator wired.
     */
    public static final class BandedCubeFillStrategy implements StackedBandStrategy {
        private final ServerLevel level;
        private final StackedDimension dim;
        private final BlockState fillState;

        BandedCubeFillStrategy(ServerLevel level, StackedDimension dim) {
            this.level = level;
            this.dim = dim;
            @Nullable BlockState resolved = BandedCubeFill.tryResolveFillBlock(dim);
            this.fillState = resolved != null ? resolved : Blocks.STONE.defaultBlockState();
        }

        @Override public StackedDimension getDimension() { return this.dim; }
        @Override public ServerLevel getLevel() { return this.level; }

        @Override
        public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
            return BandedCubeFill.fillBand(primer, this.dim, this.level, cubeX, cubeY, cubeZ, this.fillState);
        }
    }
}
