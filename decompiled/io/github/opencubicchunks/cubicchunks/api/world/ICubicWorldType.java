package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldType {
   @Nullable
   ICubeGenerator createCubeGenerator(World var1);

   IntRange calculateGenerationHeightRange(WorldServer var1);

   boolean hasCubicGeneratorForWorld(World var1);
}
