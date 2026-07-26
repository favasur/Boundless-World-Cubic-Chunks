package io.github.opencubicchunks.cubicchunks.core.entity;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.entity.player.EntityPlayerMP;

public interface ICubicEntityTracker {
   void sendLeashedEntitiesInCube(EntityPlayerMP var1, ICube var2);

   void setVertViewDistance(int var1);

   public interface Entry {
      void setMaxVertRange(int var1);
   }
}
