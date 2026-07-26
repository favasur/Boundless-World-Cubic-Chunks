package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.event;

import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator;
import net.minecraftforge.event.terraingen.InitMapGenEvent.EventType;
import net.minecraftforge.fml.common.eventhandler.Event;

public class InitCubicStructureGeneratorEvent extends Event {
   private final EventType type;
   private final ICubicStructureGenerator originalGen;
   private ICubicStructureGenerator newGen;

   public InitCubicStructureGeneratorEvent(EventType type, ICubicStructureGenerator original) {
      this.type = type;
      this.originalGen = original;
      this.setNewGen(original);
   }

   public EventType getType() {
      return this.type;
   }

   public ICubicStructureGenerator getOriginalGen() {
      return this.originalGen;
   }

   public ICubicStructureGenerator getNewGen() {
      return this.newGen;
   }

   public void setNewGen(ICubicStructureGenerator newGen) {
      this.newGen = newGen;
   }
}
