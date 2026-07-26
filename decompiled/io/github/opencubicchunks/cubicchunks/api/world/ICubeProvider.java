package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeProvider {
   @Nullable
   ICube getLoadedCube(int var1, int var2, int var3);

   @Nullable
   ICube getLoadedCube(CubePos var1);

   ICube getCube(int var1, int var2, int var3);

   ICube getCube(CubePos var1);

   @Nullable
   Chunk getLoadedColumn(int var1, int var2);

   Chunk provideColumn(int var1, int var2);
}
