package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A column of cubic chunks (a 16x16 stack of cubes along the Y axis).
 */
public interface IColumn extends XZAddressable {
    int getHeight(BlockPos pos);

    int getHeightValue(int localX, int localY, int localZ);

    boolean shouldTick();

    IHeightMap getOpacityIndex();

    Collection<? extends ICube> getLoadedCubes();

    Iterable<? extends ICube> getLoadedCubes(int minY, int maxY);

    @Nullable
    ICube getLoadedCube(int cubeY);

    ICube getCube(int cubeY);

    void addCube(ICube cube);

    @Nullable
    ICube removeCube(int cubeY);

    boolean hasLoadedCubes();

    void preCacheCube(ICube cube);
}
