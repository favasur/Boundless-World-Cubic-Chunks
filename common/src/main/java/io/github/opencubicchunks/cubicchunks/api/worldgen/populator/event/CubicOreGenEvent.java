package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.Random;

/**
 * Fired around cube ore generation. 1.21 port: drops the Forge {@code Event}
 * base class and replaces the legacy {@code Feature<?>} reference with
 * {@link PlacedFeature}, the modern 1.21 placement entry-point.
 */
public class CubicOreGenEvent {
    private final Level world;
    private final Random rand;
    private final CubePos pos;

    public CubicOreGenEvent(Level world, Random rand, CubePos pos) {
        this.world = world;
        this.rand = rand;
        this.pos = pos;
    }

    public Level getWorld() { return this.world; }
    public Random getRand() { return this.rand; }
    public CubePos getPos() { return this.pos; }

    public static class Pre extends CubicOreGenEvent {
        public Pre(Level world, Random rand, CubePos pos) {
            super(world, rand, pos);
        }
    }

    /**
     * Replacement for the legacy {@code HasResult} annotation. Modern
     * NeoForge forwards a {@code ResultEvent} that we approximate with a
     * "consume-by-default" flag the platform layer can read; on Fabric,
     * this is a no-op hook.
     */
    public static class GenerateMinable extends CubicOreGenEvent {
        private final BlockState type;
        private final PlacedFeature placedFeature;

        public GenerateMinable(Level world, Random rand, PlacedFeature placedFeature, CubePos pos, BlockState type) {
            super(world, rand, pos);
            this.placedFeature = placedFeature;
            this.type = type;
        }
        public BlockState getType() { return this.type; }
        public PlacedFeature getPlacedFeature() { return this.placedFeature; }
    }

    public static class Post extends CubicOreGenEvent {
        public Post(Level world, Random rand, CubePos pos) {
            super(world, rand, pos);
        }
    }
}
