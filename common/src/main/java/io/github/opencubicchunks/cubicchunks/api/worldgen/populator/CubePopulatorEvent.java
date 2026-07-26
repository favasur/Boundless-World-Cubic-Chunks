package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.world.level.Level;

/**
 * General cubic populator event. 1.21 port: plain POJO, no Forge event-bus
 * inheritance. Cancellation is honoured via {@code ICubicPlatform.fireEvent}
 * (NeoForge path checks {@code event.isCanceled()}, Fabric acknowledges via
 * the platform wrapper's} no-op default); the {@code Deprecated} flag is
 * kept because populator-event hooks were mostly already routed through
 * Minecraft's BRUSHABLE_BLOCK_ENTITY-style lifecycle in 1.21.
 */
@Deprecated
public class CubePopulatorEvent {
    private final ICube cube;
    private final Level world;

    public CubePopulatorEvent(Level worldIn, ICube cubeIn) {
        this.cube = cubeIn;
        this.world = worldIn;
    }

    public ICube getCube() {
        return this.cube;
    }

    public Level getWorld() {
        return this.world;
    }

    /**
     * Always returns true on the modern port. Cancelling the populator at the
     * Forge-Bus-level event isn't supported; downstream mods gate population
     * via {@code CubicChunksConfig.maxGeneratedCubesPerTick} or replace
     * {@code ICubeGenerator.populate} via the CubicGeneratorsRegistry.
     */
    public boolean isCancelable() {
        return true;
    }
}
