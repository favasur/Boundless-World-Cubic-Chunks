package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ICubicFeatureGenerator extends ICubicStructureGenerator {
   String getStructureName();

   @Override
   void generate(World var1, @Nullable CubePrimer var2, CubePos var3);

   boolean generateStructure(World var1, Random var2, CubePos var3);

   boolean isInsideStructure(World var1, BlockPos var2);

   boolean isPositionInStructure(World var1, BlockPos var2);

   @Nullable
   BlockPos getNearestStructurePos(World var1, BlockPos var2, boolean var3);
}
