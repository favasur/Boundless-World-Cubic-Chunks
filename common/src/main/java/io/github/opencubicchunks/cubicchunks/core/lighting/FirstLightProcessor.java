package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor
// 1.21 minimal implementation: use the vanilla LevelLightEngine to recalculate both
// block and sky light for every block in a freshly generated cube. This is slower than
// the original optimized BFS but is correct and avoids dark cubes on first load.
public class FirstLightProcessor {
    private final Level level;

    public FirstLightProcessor(Level level) {
        this.level = level;
    }

    public static FirstLightProcessor forLevel(Level level) {
        return new FirstLightProcessor(level);
    }

    public void diffuseSkylight(Cube cube) {
        if (cube.getWorld() != this.level) {
            throw new IllegalArgumentException("Cube does not belong to this level");
        }
        if (this.level.isClientSide()) {
            return;
        }
        if (cube.isEmpty()) {
            return;
        }

        // Stacked-band cubes whose entire Y range lives above the overworld's
        // getMaxBuildHeight() (=320) or below getMinBuildHeight() (=-64) are out of
        // reach of the vanilla LevelLightEngine. The engine's section arrays are
        // sized at construction time to [min, max], so any checkBlock above max or
        // below min is silently dropped. Short-circuit here so we never spend 4096
        // iterations per cube re-stamping positions the engine can't index — and so
        // we never give a future regression a path to push light downward from a
        // stacked band into the overworld surface.
        int cubeMinY = cube.getCoords().getMinBlockY();
        int cubeMaxY = cube.getCoords().getMaxBlockY();
        if (cubeMinY >= this.level.getMaxBuildHeight() || cubeMaxY < this.level.getMinBuildHeight()) {
            return;
        }

        var lightEngine = this.level.getLightEngine();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minX = cube.getCoords().getMinBlockX();
        int minY = cubeMinY;
        int minZ = cube.getCoords().getMinBlockZ();

        // Skip the bottom rows of the iteration if the cube starts above minBuildHeight
        // (relevant when stacking straddles the boundary), and the top rows if it ends
        // above maxBuildHeight. Saves CPU on the few cubes that clip the Overworld ceiling.
        int startY = Math.max(0, this.level.getMinBuildHeight() - minY);
        int endY = Math.min(16, this.level.getMaxBuildHeight() - minY);
        if (startY >= endY) {
            return;
        }

        for (int y = startY; y < endY; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    mutable.set(minX + x, minY + y, minZ + z);
                    lightEngine.checkBlock(mutable);
                }
            }
        }
    }
}
