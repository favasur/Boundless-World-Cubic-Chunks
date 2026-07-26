package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicTicket;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicTicketInternal extends ICubicTicket, ITicket {
   void addRequestedCube(CubePos var1);

   void removeRequestedCube(CubePos var1);

   void setForcedChunkCubes(ChunkPos var1, IntSet var2);

   void clearForcedChunkCubes(ChunkPos var1);

   void setAllForcedChunkCubes(Map<ChunkPos, IntSet> var1);

   void setModData(NBTTagCompound var1);

   void setPlayer(String var1);

   void setEntityChunkX(int var1);

   void setEntityChunkY(int var1);

   void setEntityChunkZ(int var1);

   int getEntityChunkX();

   int getEntityChunkY();

   int getEntityChunkZ();

   int getMaxCubeDepth();

   @Override
   default boolean shouldTick() {
      return true;
   }

   Set<CubePos> requestedCubes();
}
