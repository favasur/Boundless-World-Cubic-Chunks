package io.github.opencubicchunks.cubicchunks.core.world.column;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap
// 1.21: extended with single-int cubeY accessors for the IColumn-shaped mixin
// call sites, with a {@link Collection}-returning {@link #all()} view, and
// preserved the full-pos accessor set for callers that already know the column.
public class CubeMap {
    private final Long2ObjectMap<Cube> map = new Long2ObjectOpenHashMap<>();

    public void put(ICube cube) {
        if (cube instanceof Cube serverCube) {
            map.put(serverCube.getCoords().asLong(), serverCube);
        }
    }

    public Cube get(CubePos pos) {
        return map.get(pos.asLong());
    }

    /**
     * Lookup by cubeY alone. Used by column-bound callers (MixinLevelChunk, EmptyColumn)
     * that already know the column's x/z coordinates and only need the per-cube-Y
     * cube. Linear scan but only inside the column's own map (already small).
     */
    public Cube get(int cubeY) {
        for (Cube cube : map.values()) {
            if (cube.getCoords().getY() == cubeY) {
                return cube;
            }
        }
        return null;
    }

    public Cube get(int cubeX, int cubeY, int cubeZ) {
        return map.get(CubePos.of(cubeX, cubeY, cubeZ).asLong());
    }

    public Cube remove(CubePos pos) {
        return map.remove(pos.asLong());
    }

    public Cube remove(int cubeY) {
        Cube found = null;
        for (Cube cube : map.values()) {
            if (cube.getCoords().getY() == cubeY) {
                found = cube;
                break;
            }
        }
        if (found != null) {
            map.remove(found.getCoords().asLong());
        }
        return found;
    }

    public Cube remove(int cubeX, int cubeY, int cubeZ) {
        return map.remove(CubePos.of(cubeX, cubeY, cubeZ).asLong());
    }

    public boolean contains(CubePos pos) {
        return map.containsKey(pos.asLong());
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    public Iterable<Cube> values() {
        return map.values();
    }

    public Collection<Cube> all() {
        return Collections.unmodifiableCollection(map.values());
    }

    public Iterable<Cube> cubes(int minCubeY, int maxCubeY) {
        List<Cube> out = new ArrayList<>();
        for (Cube cube : map.values()) {
            int y = cube.getCoords().getY();
            if (y >= minCubeY && y <= maxCubeY) {
                out.add(cube);
            }
        }
        return out;
    }
}
