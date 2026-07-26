package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/**
 * Pre-light staging height map for pending cubes. 1.21 port: implements the
 * IHeightMap.isEmpty(int, int) entry-point that 1.21 IHeightMap promoted
 * from optional to required. We return the dirty-bit-clear heightmap[]
 * sentinel since staging genuinely has no opaque blocks until the
 * containing cubes finish their surface tracking.
 */
public class StagingHeightMap implements IHeightMap {
    private final List<ICube> stagedCubes = new ArrayList<>();
    private final int[] heightmap = new int[256];
    private final BitSet dirtyFlag = new BitSet(this.heightmap.length);

    public StagingHeightMap() {
    }

    public void addStagedCube(ICube cube) {
        this.stagedCubes.add(cube);
        this.stagedCubes.sort(Comparator.comparingInt(c -> -c.getCoords().getY()));
        if (!cube.isEmpty()) {
            this.dirtyFlag.set(0, this.heightmap.length);
        }
    }

    public void removeStagedCube(ICube cube) {
        if (this.stagedCubes.remove(cube) && !cube.isEmpty()) {
            this.dirtyFlag.set(0, this.heightmap.length);
        }
    }

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        if (opacity > 0) {
            if (blockY > getTopBlockY(localX, localZ)) {
                this.heightmap[index(localX, localZ)] = blockY;
            }
        } else if (blockY == getTopBlockY(localX, localZ)) {
            this.dirtyFlag.set(index(localX, localZ));
        }
    }

    private int index(int localX, int localZ) {
        return localZ << 4 | localX;
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        int idx = index(localX, localZ);
        if (!this.dirtyFlag.get(idx)) {
            return this.heightmap[idx];
        }
        this.dirtyFlag.clear(idx);
        return this.heightmap[idx] = computeHeightMap(localX, localZ);
    }

    private int computeHeightMap(int localX, int localZ) {
        for (ICube cube : this.stagedCubes) {
            LevelChunkSection storage = ((io.github.opencubicchunks.cubicchunks.core.world.cube.Cube) cube).getStorage();
            if (storage != null && !storage.hasOnlyAir()) {
                for (int i = 15; i >= 0; i--) {
                    if (storage.getBlockState(localX, i, localZ).getLightEmission() > 0) {
                        return Coords.localToBlock(cube.getY(), i);
                    }
                }
            }
        }
        return Integer.MIN_VALUE / 2;
    }

    @Override
    public boolean isEmpty(int localX, int localZ) {
        // Staging has no opaque blocks until surface tracking runs; treat any
        // untouched cell (height sentinel) as empty. Once a real height is
        // computed, the cell is non-empty.
        return getTopBlockY(localX, localZ) == Integer.MIN_VALUE / 2;
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        throw new UnsupportedOperationException("Not implemented for staging heightmap");
    }

    @Override
    public int getLowestTopBlockY() {
        throw new UnsupportedOperationException("Not implemented for staging heightmap");
    }
}
