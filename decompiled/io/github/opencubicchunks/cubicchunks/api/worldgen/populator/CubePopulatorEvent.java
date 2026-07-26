package io.github.opencubicchunks.cubicchunks.api.worldgen.populator;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
@Deprecated
public class CubePopulatorEvent extends Event {
   private final ICube cube;
   private final World world;

   public CubePopulatorEvent(World worldIn, ICube cubeIn) {
      this.cube = cubeIn;
      this.world = worldIn;
   }

   public ICube getCube() {
      return this.cube;
   }

   public World getWorld() {
      return this.world;
   }

   public boolean isCancelable() {
      return true;
   }
}
