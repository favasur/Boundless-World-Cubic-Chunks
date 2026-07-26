package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeProviderInternal extends ICubeProvider {
   @Nullable
   Cube getLoadedCube(int var1, int var2, int var3);

   @Nullable
   Cube getLoadedCube(CubePos var1);

   Cube getCube(int var1, int var2, int var3);

   Cube getCube(CubePos var1);

   public interface Server extends ICubeProviderInternal {
      ICubeIO getCubeIO();
   }
}
