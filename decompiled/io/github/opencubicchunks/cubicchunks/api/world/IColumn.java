package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import java.util.Collection;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IColumn extends XZAddressable {
   int getHeight(BlockPos var1);

   @Deprecated
   int getHeightValue(int var1, int var2);

   int getHeightValue(int var1, int var2, int var3);

   boolean shouldTick();

   IHeightMap getOpacityIndex();

   Collection<? extends ICube> getLoadedCubes();

   Iterable<? extends ICube> getLoadedCubes(int var1, int var2);

   @Nullable
   ICube getLoadedCube(int var1);

   ICube getCube(int var1);

   void addCube(ICube var1);

   @Nullable
   ICube removeCube(int var1);

   boolean hasLoadedCubes();

   void preCacheCube(ICube var1);
}
