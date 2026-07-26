package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.world.level.Level;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal
public interface IColumnInternal extends IColumn {
    CubePrimer getCompatGenerationPrimer();

    void removeFromStagingHeightmap(ICube cube);

    void addToStagingHeightmap(ICube cube);

    int getHeightWithStaging(int localX, int localZ);

    Level getWorld();
}
