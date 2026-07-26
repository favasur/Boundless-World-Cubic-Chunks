package io.github.opencubicchunks.cubicchunks.core.world;

import javax.annotation.Nullable;
import net.minecraft.world.WorldServer;

public interface IWorldEntitySpawner {
   int findChunksForSpawning(WorldServer var1, boolean var2, boolean var3, boolean var4);

   public interface Handler {
      void setEntitySpawner(@Nullable IWorldEntitySpawner var1);

      @Nullable
      IWorldEntitySpawner getEntitySpawner();
   }
}
