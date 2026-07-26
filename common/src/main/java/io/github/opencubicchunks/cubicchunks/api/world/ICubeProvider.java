package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

/**
 * Provides access to cubic chunks (loaded or generated on demand).
 */
public interface ICubeProvider {
    @Nullable
    ICube getLoadedCube(int cubeX, int cubeY, int cubeZ);

    @Nullable
    default ICube getLoadedCube(CubePos pos) {
        return getLoadedCube(pos.getX(), pos.getY(), pos.getZ());
    }

    ICube getCube(int cubeX, int cubeY, int cubeZ);

    default ICube getCube(CubePos pos) {
        return getCube(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    ChunkAccess getLoadedColumn(int columnX, int columnZ);

    ChunkAccess provideColumn(int columnX, int columnZ);

    default void tick(BooleanSupplier hasMoreTime) {
        // Loader-specific implementations override this to process async tasks.
    }

    default void close() {
        // Loader-specific implementations override this to release IO resources.
    }

    default void markForRenderUpdate(CubePos pos) {
        // Client-side providers override this to dirty the relevant render section.
    }

    /**
     * Registers a cube that was created on-demand (e.g., by block placement) with this provider.
     */
    default void addLoadedCube(ICube cube) {
        // Loader-specific server/client providers implement this.
    }
}
