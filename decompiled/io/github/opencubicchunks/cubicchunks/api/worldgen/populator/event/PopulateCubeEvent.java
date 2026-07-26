package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType;
import net.minecraftforge.fml.common.eventhandler.Event.HasResult;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PopulateCubeEvent extends CubeGeneratorEvent {
   private final World world;
   private final Random rand;
   private final int cubeX;
   private final int cubeY;
   private final int cubeZ;
   private final boolean hasVillageGenerated;

   public PopulateCubeEvent(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
      super(((ICubicWorldServer)world).getCubeGenerator());
      this.world = world;
      this.rand = rand;
      this.cubeX = cubeX;
      this.cubeY = cubeY;
      this.cubeZ = cubeZ;
      this.hasVillageGenerated = hasVillageGenerated;
   }

   public World getWorld() {
      return this.world;
   }

   public Random getRand() {
      return this.rand;
   }

   public int getCubeX() {
      return this.cubeX;
   }

   public int getCubeY() {
      return this.cubeY;
   }

   public int getCubeZ() {
      return this.cubeZ;
   }

   public boolean isHasVillageGenerated() {
      return this.hasVillageGenerated;
   }

   @HasResult
   public static class Populate extends PopulateCubeEvent {
      private final EventType type;

      public EventType getType() {
         return this.type;
      }

      public Populate(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated, EventType type) {
         super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
         this.type = type;
      }
   }

   public static class Post extends PopulateCubeEvent {
      public Post(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
         super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
      }
   }

   public static class Pre extends PopulateCubeEvent {
      public Pre(World world, Random rand, int cubeX, int cubeY, int cubeZ, boolean hasVillageGenerated) {
         super(world, rand, cubeX, cubeY, cubeZ, hasVillageGenerated);
      }
   }
}
