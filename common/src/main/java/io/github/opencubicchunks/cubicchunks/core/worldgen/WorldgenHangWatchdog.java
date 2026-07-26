package io.github.opencubicchunks.cubicchunks.core.worldgen;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.worldgen.WorldgenHangWatchdog
// 1.21: simplified watchdog. Tracks per-thread worldgen start/stop via a sentinel counter
// so the server thread can detect a generator that didn't return within `HUNG_SECONDS`.
public class WorldgenHangWatchdog {

    public static final boolean ENABLED = "true".equalsIgnoreCase(System.getProperty("cubicchunks.wgen_hang_watchdog", "true"));
    private static final long HUNG_SECONDS = Long.getLong("cubicchunks.wgen_hung_seconds", 30);

    private final Thread thread;
    private final long startMillis;
    private volatile boolean finished = false;

    private WorldgenHangWatchdog(Thread thread) {
        this.thread = thread;
        this.startMillis = System.currentTimeMillis();
    }

    public static WorldgenHangWatchdog start(String tag) {
        if (!ENABLED) return new WorldgenHangWatchdog(Thread.currentThread());
        CubicChunks.LOGGER.debug("WorldgenHangWatchdog start: {} on {}", tag, Thread.currentThread().getName());
        return new WorldgenHangWatchdog(Thread.currentThread());
    }

    public void finish() {
        this.finished = true;
        if (ENABLED) {
            CubicChunks.LOGGER.debug("WorldgenHangWatchdog finish: took {} ms",
                    System.currentTimeMillis() - this.startMillis);
        }
    }

    public boolean checkHung() {
        if (this.finished) return false;
        long elapsed = (System.currentTimeMillis() - this.startMillis) / 1000;
        return elapsed > HUNG_SECONDS;
    }

    public Thread getThread() {
        return this.thread;
    }
}
