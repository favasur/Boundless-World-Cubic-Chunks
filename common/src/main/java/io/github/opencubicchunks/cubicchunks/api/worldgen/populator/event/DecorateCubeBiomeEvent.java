package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Decorate-phase event for cubic biomes. 1.21 port: drops the Forge event bus
 * and the missing {@code net.minecraftforge.event.terraingen.DecorateBiomeEvent}
 * reference. We replace it with a small integer {@link Type} enum so callers
 * can still discriminate decoration kinds without depending on the legacy
 * terraingen hierarchy that has no clean NeoForge equivalent any more.
 */
public class DecorateCubeBiomeEvent {
    /**
     * Lightweight stand-in for {@code DecorateBiomeEvent.Decorate.EventType}.
     * Cube population does not need the full Forge decoration taxonomy since
     * ModLoader-level integrations have moved to Minecraft's own lifecycle.
     */
    public enum Type {
        ANIMALS,
        DUNGEONS,
        WATER_LAKE,
        LAVA_LAKE,
        ORE,
        DECORATION,
        TREES,
        FLOWERS,
        MUSHROOMS,
        PLANTS,
        FOSSILS,
        ICE
    }

    private final Level world;
    private final Random rand;
    private final CubePos cubePos;

    public DecorateCubeBiomeEvent(Level world, Random rand, CubePos cubePos) {
        this.world = world;
        this.rand = rand;
        this.cubePos = cubePos;
    }

    public Level getWorld() { return this.world; }
    public Random getRand() { return this.rand; }
    public CubePos getCubePos() { return this.cubePos; }

    public static class Pre extends DecorateCubeBiomeEvent {
        public Pre(Level world, Random rand, CubePos cubePos) {
            super(world, rand, cubePos);
        }
    }

    /** Replaces the legacy {@code HasResult}-annotated {@code Decorate} variant. */
    public static class Decorate extends DecorateCubeBiomeEvent {
        private final Type type;
        @Nullable
        private final BlockPos placementPos;

        public Decorate(Level world, Random rand, CubePos cubePos,
                        @Nullable BlockPos placementPos,
                        Type type) {
            super(world, rand, cubePos);
            this.placementPos = placementPos;
            this.type = type;
        }
        @Nullable
        public BlockPos getPlacementPos() { return this.placementPos; }
        public Type getType() { return this.type; }
    }

    public static class Post extends DecorateCubeBiomeEvent {
        public Post(Level world, Random rand, CubePos cubePos) {
            super(world, rand, cubePos);
        }
    }
}
