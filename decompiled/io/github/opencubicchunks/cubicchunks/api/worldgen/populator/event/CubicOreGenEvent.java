package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.Event.HasResult;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubicOreGenEvent extends Event {
   private final World world;
   private final Random rand;
   private final CubePos pos;

   public CubicOreGenEvent(World world, Random rand, CubePos pos) {
      this.world = world;
      this.rand = rand;
      this.pos = pos;
   }

   public World getWorld() {
      return this.world;
   }

   public Random getRand() {
      return this.rand;
   }

   public CubePos getPos() {
      return this.pos;
   }

   @HasResult
   public static class GenerateMinable extends CubicOreGenEvent {
      private final IBlockState type;
      private final WorldGenerator generator;

      public GenerateMinable(World world, Random rand, WorldGenerator generator, CubePos pos, IBlockState type) {
         super(world, rand, pos);
         this.generator = generator;
         this.type = type;
      }

      public IBlockState getType() {
         return this.type;
      }

      public WorldGenerator getGenerator() {
         return this.generator;
      }
   }

   public static class Post extends CubicOreGenEvent {
      public Post(World world, Random rand, CubePos pos) {
         super(world, rand, pos);
      }
   }

   public static class Pre extends CubicOreGenEvent {
      public Pre(World world, Random rand, CubePos pos) {
         super(world, rand, pos);
      }
   }
}
