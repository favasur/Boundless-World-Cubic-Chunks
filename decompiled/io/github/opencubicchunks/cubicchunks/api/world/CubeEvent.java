package io.github.opencubicchunks.cubicchunks.api.world;

import net.minecraftforge.event.world.WorldEvent;

public class CubeEvent extends WorldEvent {
   private final ICube chunk;

   public CubeEvent(ICube cube) {
      super(cube.getWorld());
      this.chunk = cube;
   }

   public ICube getCube() {
      return this.chunk;
   }

   public static class Load extends CubeEvent {
      public Load(ICube cube) {
         super(cube);
      }
   }

   public static class Unload extends CubeEvent {
      public Unload(ICube cube) {
         super(cube);
      }
   }
}
