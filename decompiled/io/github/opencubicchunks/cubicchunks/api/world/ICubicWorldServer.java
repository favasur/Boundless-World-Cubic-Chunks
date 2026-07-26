package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldServer extends ICubicWorld {
   ICubeProviderServer getCubeCache();

   ICubeGenerator getCubeGenerator();

   void unloadOldCubes();

   void forceChunk(Ticket var1, CubePos var2);

   void reorderChunk(Ticket var1, CubePos var2);

   void unforceChunk(Ticket var1, CubePos var2);
}
