package io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.fml.common.eventhandler.Event;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeGeneratorEvent extends Event {
   private final ICubeGenerator gen;

   public CubeGeneratorEvent(ICubeGenerator gen) {
      this.gen = gen;
   }

   public ICubeGenerator getGenerator() {
      return this.gen;
   }
}
