package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import javax.annotation.Nullable;
import java.util.function.Consumer;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor
// TODO: replace with a real async I/O executor backed by 1.21 chunk loading.
public class AsyncWorldIOExecutor {

    @Nullable
    public static Cube syncCubeLoad(Level level, ICubeIO loader, Object cache, int cubeX, int cubeY, int cubeZ) {
        // 1.21: synchronous load stub. Real implementation should queue and run async tasks.
        return null;
    }

    @Nullable
    public static ChunkAccess syncColumnLoad(Level level, ICubeIO loader, int x, int z, Consumer<ChunkAccess> setLoadingColumnCallback) {
        return null;
    }

    public static void queueCubeLoad(Level level, ICubeIO loader, Object cache, int x, int y, int z, Consumer<Cube> runnable) {
    }

    public static void queueColumnLoad(Level level, ICubeIO loader, int x, int z, Consumer<ChunkAccess> runnable, Consumer<ChunkAccess> setLoadingColumnCallback) {
    }

    public static void dropQueuedCubeLoad(Level level, int x, int y, int z, Consumer<Cube> runnable) {
    }

    public static void dropQueuedColumnLoad(Level level, int x, int z, Consumer<ChunkAccess> runnable) {
    }

    public static void tick() {
    }

    public static boolean canDropColumn(Level level, int x, int z) {
        return true;
    }
}
