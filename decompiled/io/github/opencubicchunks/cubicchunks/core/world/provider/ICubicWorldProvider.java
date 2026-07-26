package io.github.opencubicchunks.cubicchunks.core.world.provider;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldProvider {
   @Nullable
   ICubeGenerator createCubeGenerator();

   int getOriginalActualHeight();

   World getWorld();
}
