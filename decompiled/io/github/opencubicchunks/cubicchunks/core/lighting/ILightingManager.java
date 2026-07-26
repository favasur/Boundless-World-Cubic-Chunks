package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ILightingManager {
   void doOnBlockSetLightUpdates(Chunk var1, int var2, int var3, int var4, int var5);

   void onTick();

   void markCubeBlockColumnForUpdate(ICube var1, int var2, int var3);

   boolean checkLightFor(EnumSkyBlock var1, BlockPos var2);
}
