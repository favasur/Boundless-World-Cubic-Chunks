package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.minecraft.world.chunk.ChunkPrimer;

public interface IColumnInternal extends IColumn {
   ChunkPrimer getCompatGenerationPrimer();

   void removeFromStagingHeightmap(ICube var1);

   void addToStagingHeightmap(ICube var1);

   int getHeightWithStaging(int var1, int var2);
}
