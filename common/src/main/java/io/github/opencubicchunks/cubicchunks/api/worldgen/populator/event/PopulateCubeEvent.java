package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * Fired around cube population. 1.21 port: drops the Forge event bus base
 * class and the missing {@code net.minecraftforge.event.terraingen.PopulateChunkEvent}
 * reference. We keep a tiny PopulateType taxonomy so existing subscribers
 * still discriminate the population phase without depending on terraingen.
 */
public class PopulateCubeEvent extends CubeGeneratorEvent {

    /** Light-weight stand-in for {@code PopulateChunkEvent.Populate.EventType}. */
    public enum PopulateType {
        ANIMALS,
        DUNGEONS,
        ICE,
        LIGHT,
        NETHER,
        PLANTS,
        STRUCTURES,
        STRUCTURE_REFERENCES,
        SURFACE,
        UNDERGROUND
    }

    private final Level world;
    private final Random rand;
    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;
    private final boolean hasVillageGenerated;

    public PopulateCubeEvent(Level world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
        super(world instanceof io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer server
                ? server.getCubeGenerator()
                : null);
        this.world = world;
        this.rand = rand;
        this.cubeX = cubeX;
        this.cubeY = cubeY;
        this.cubeZ = cubeZ;
        this.hasVillageGenerated = hasVillageGenerated;
    }

    public Level getWorld() { return this.world; }
    public Random getRand() { return this.rand; }
    public int getCubeX() { return this.cubeX; }
    public int getCubeY() { return this.cubeY; }
    public int getCubeZ() { return this.cubeZ; }
    public boolean isHasVillageGenerated() { return this.hasVillageGenerated; }

    public static class Pre extends PopulateCubeEvent {
        public Pre(Level world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
        }
    }

    /** Replaces the legacy {@code HasResult}-annotated {@code Populate} variant. */
    public static class Populate extends PopulateCubeEvent {
        private final PopulateType type;

        public Populate(Level world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated,
                        PopulateType type) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
            this.type = type;
        }
        public PopulateType getType() { return this.type; }
    }

    public static class Post extends PopulateCubeEvent {
        public Post(Level world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
            super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
        }
    }
}
