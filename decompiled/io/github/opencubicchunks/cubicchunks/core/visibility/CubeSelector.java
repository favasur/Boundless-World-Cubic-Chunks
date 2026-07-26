package io.github.opencubicchunks.cubicchunks.core.visibility;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.ChunkPos;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubeSelector {
   public CubeSelector() {
   }

   public abstract void forAllVisibleFrom(CubePos var1, int var2, int var3, Consumer<CubePos> var4);

   public abstract void findChanged(
      CubePos var1, CubePos var2, int var3, int var4, Set<CubePos> var5, Set<CubePos> var6, Set<ChunkPos> var7, Set<ChunkPos> var8
   );

   public abstract void findAllUnloadedOnViewDistanceDecrease(CubePos var1, int var2, int var3, int var4, int var5, Set<CubePos> var6, Set<ChunkPos> var7);
}
