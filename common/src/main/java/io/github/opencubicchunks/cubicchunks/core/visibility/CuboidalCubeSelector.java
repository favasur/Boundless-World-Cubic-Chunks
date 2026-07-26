package io.github.opencubicchunks.cubicchunks.core.visibility;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.visibility.CuboidalCubeSelector
// 1.21: 3D Euclidean sphere is preserved; this matches `computeDesiredCubes`.
public class CuboidalCubeSelector {

    private final int distance;

    public CuboidalCubeSelector(int distance) {
        this.distance = distance;
    }

    public boolean test(int originCubeX, int originCubeY, int originCubeZ, CubePos pos) {
        int dx = pos.getX() - originCubeX;
        int dy = pos.getY() - originCubeY;
        int dz = pos.getZ() - originCubeZ;
        return dx * dx + dy * dy + dz * dz <= this.distance * this.distance;
    }

    public int getDistance() {
        return this.distance;
    }

    public static CuboidalCubeSelector ofPlayer(int cubeX, int cubeY, int cubeZ, int viewDistance) {
        return new CuboidalCubeSelector(viewDistance);
    }

    public static int toCubeY(int blockY) {
        return Coords.blockToCube(blockY);
    }
}
