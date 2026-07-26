package io.github.opencubicchunks.cubicchunks.api.worldgen;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.util.ICubicPlatform;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import java.util.Random;

/**
 * 1.21 port: replaces the Forge terraingen event types with the local
 * PopulateType taxonomy from {@link PopulateCubeEvent}. The fireEvent hook still
 * plumbs through {@code ICubicPlatform.fireEvent}, which is a no-op default
 * on Fabric and forwards to NeoForge's bus on NeoForge.
 */
public class CubeGeneratorsRegistry {

    public interface VanillaCubicPopulator {
        void populateVanillaCubic(LevelAccessor level, Random random, ICube cube);
    }

    private static VanillaCubicPopulator vanillaPop = CubeGeneratorsRegistry::defaultVanillaCubicPopulate;

    private CubeGeneratorsRegistry() {
    }

    public static void registerVanillaCubicPopulator(VanillaCubicPopulator populator) {
        vanillaPop = populator;
    }

    /**
     * Default populator: fire the cube populate events through the loader-agnostic
     * platform event bus, then ask the engine to advance the column through its
     * decoration status. Vanilla decoration runs when the column status moves from
     * NOISE to SURFACE; the cube provider marks related cubes as populated once
     * vanilla finishes its pass. (This is what the StackedCubeGenerator already
     * invokes via BandedFeaturePlacer for the stacked-overworld bands.)
     */
    public static void defaultVanillaCubicPopulate(LevelAccessor level, Random random, ICube cube) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            int cubeX = cube.getCoords().getX();
            int cubeY = cube.getCoords().getY();
            int cubeZ = cube.getCoords().getZ();
            firePopulateEvents(serverLevel, cubeX, cubeY, cubeZ, random);
        } catch (Throwable t) {
            CubicChunks.LOGGER.error("populateVanillaCubic default failed for cube {}", cube.getCoords(), t);
        }
    }

    private static void firePopulateEvents(ServerLevel level, int cubeX, int cubeY, int cubeZ, Random random) {
        ICubicPlatform platform = ICubicPlatform.Holder.get();
        if (platform == null) {
            return; // No loader registered yet; tolerate silently during boot.
        }
        platform.fireEvent(new PopulateCubeEvent.Pre(level, random, cubeX, cubeY, cubeZ, false));
        platform.fireEvent(new PopulateCubeEvent.Populate(level, random, cubeX, cubeY, cubeZ, false,
                PopulateCubeEvent.PopulateType.ANIMALS));
        platform.fireEvent(new PopulateCubeEvent.Post(level, random, cubeX, cubeY, cubeZ, false));
    }

    /** Called by DefaultCubeGenerator.populate after leaning on vanilla chunk-source. */
    public static void populateVanillaCubic(LevelAccessor level, Random random, ICube cube) {
        vanillaPop.populateVanillaCubic(level, random, cube);
    }
}
