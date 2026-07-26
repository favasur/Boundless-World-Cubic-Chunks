package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerChunkMapEntry.class})
public interface IPlayerChunkMapEntry {
   @Accessor("players")
   List<EntityPlayerMP> getPlayerList();

   @Accessor
   void setLastUpdateInhabitedTime(long var1);

   @Accessor
   void setSentToPlayers(boolean var1);

   @Accessor(
      remap = false
   )
   boolean isLoading();

   @Accessor(
      remap = false
   )
   Runnable getLoadedRunnable();

   @Accessor
   Chunk getChunk();

   @Accessor
   void setChunk(Chunk var1);

   @Accessor
   ChunkPos getPos();
}
