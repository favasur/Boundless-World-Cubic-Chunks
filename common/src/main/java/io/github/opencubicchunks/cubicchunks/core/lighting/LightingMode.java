package io.github.opencubicchunks.cubicchunks.core.lighting;

// @Original: additive port from 1.12.2 (the original had no explicit enum — the
// async-batched dispatch pattern lived in CubeLightEngine; we surface it as a
// config flag so users can opt in without modifying code).
public enum LightingMode {
    /**
     * Original 1.21.x port path: {@code LightingManager.onTick()} walks the
     * {@code toUpdate} set sequentially on the calling thread. Deterministic,
     * zero overhead beyond the work itself. Identical behaviour to the previous
     * port with this flag absent.
     */
    SYNC,
    /**
     * Opt-in additive mode: {@code onTick()} dispatches each
     * {@code CubeLightUpdateInfo.tick()} to {@code Util.backgroundExecutor()}
     * via {@code CompletableFuture.runAsync(...)} and
     * {@code CompletableFuture.allOf(...).join()}s before continuing. Helpful
     * for gigacube worlds where many cubes need relighting per tick.
     */
    ASYNC_BATCHED
}
