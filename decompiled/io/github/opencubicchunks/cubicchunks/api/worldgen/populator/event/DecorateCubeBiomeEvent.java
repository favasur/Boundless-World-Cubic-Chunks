package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.Random;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.Event.HasResult;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DecorateCubeBiomeEvent extends Event {
   private final World world;
   private final Random rand;
   private final CubePos cubePos;

   public DecorateCubeBiomeEvent(World world, Random rand, CubePos cubePos) {
      this.world = world;
      this.rand = rand;
      this.cubePos = cubePos;
   }

   public World getWorld() {
      return this.world;
   }

   public Random getRand() {
      return this.rand;
   }

   public CubePos getCubePos() {
      return this.cubePos;
   }

   @HasResult
   public static class Decorate extends DecorateCubeBiomeEvent {
      private final EventType type;
      @Nullable
      private final BlockPos placementPos;

      public Decorate(World world, Random rand, CubePos cubePos, @Nullable BlockPos placementPos, EventType type) {
         super(world, rand, cubePos);
         this.type = type;
         this.placementPos = placementPos;
      }

      public EventType getType() {
         return this.type;
      }

      @Nullable
      public BlockPos getPlacementPos() {
         return this.placementPos;
      }
   }

   public static class Post extends DecorateCubeBiomeEvent {
      public Post(World world, Random rand, CubePos cubePos) {
         super(world, rand, cubePos);
      }
   }

   public static class Pre extends DecorateCubeBiomeEvent {
      public Pre(World world, Random rand, CubePos cubePos) {
         super(world, rand, cubePos);
      }
   }
}
