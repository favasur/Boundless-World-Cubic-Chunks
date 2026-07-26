package io.github.opencubicchunks.cubicchunks.api.world;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeProviderServer extends ICubeProvider {
   @Nullable
   Chunk getColumn(int var1, int var2, ICubeProviderServer.Requirement var3);

   @Nullable
   ICube getCube(int var1, int var2, int var3, ICubeProviderServer.Requirement var4);

   @Nullable
   ICube getCubeNow(int var1, int var2, int var3, ICubeProviderServer.Requirement var4);

   boolean isCubeGenerated(int var1, int var2, int var3);

   public static enum Requirement {
      GET_CACHED,
      LOAD,
      GENERATE,
      POPULATE,
      LIGHT;

      private Requirement() {
      }
   }
}
