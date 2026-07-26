package io.github.opencubicchunks.cubicchunks.core.server;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.server.ChunkGc
// 1.21: simplified cube GC. Iterates loaded cubes periodically and unloads those with
// no outstanding tickets. Throttled to one pass every `chunkGCInterval` ticks.
public class ChunkGc {

    public static final int DEFAULT_INTERVAL = 600;

    private final CubeProviderServer provider;
    private final int interval;
    private int tickCount = 0;

    public ChunkGc(CubeProviderServer provider) {
        this(provider, DEFAULT_INTERVAL);
    }

    public ChunkGc(CubeProviderServer provider, int interval) {
        this.provider = provider;
        this.interval = Math.max(60, interval);
    }

    public void tick() {
        this.tickCount++;
        if (this.tickCount < this.interval) return;
        this.tickCount = 0;
        this.gc();
    }

    public int gc() {
        int unloaded = 0;
        var cubeIter = this.provider.cubesIterator();
        while (cubeIter.hasNext()) {
            if (this.provider.tryUnloadCube(cubeIter.next())) {
                cubeIter.remove();
                unloaded++;
            }
        }
        return unloaded;
    }
}
