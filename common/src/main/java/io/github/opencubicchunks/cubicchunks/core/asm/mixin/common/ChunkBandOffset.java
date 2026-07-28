package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

/**
 * Thread-local Y-band offset used during vanilla world-gen calls against
 * stacked dimension bands. Vanilla generators write at Y=[0..255] but our
 * End band sits at Y=[12320..12832]; the offset shifts writes into the
 * correct cube range.
 *
 * <p>Extracted from {@link MixinLevelChunk} because Mixin forbids non-private
 * static methods on mixin classes.</p>
 */
public final class ChunkBandOffset {
    private static final ThreadLocal<Integer> BAND_Y_OFFSET = new ThreadLocal<>();

    private ChunkBandOffset() {}

    /**
     * Push a band Y offset onto the thread-local, returning the previous
     * value so callers can restore it in a finally block.
     */
    public static Integer push(Integer offset) {
        Integer prev = BAND_Y_OFFSET.get();
        BAND_Y_OFFSET.set(offset);
        return prev;
    }

    /** Remove the current band Y offset. */
    public static void clear() {
        BAND_Y_OFFSET.remove();
    }

    /**
     * Read the current band Y offset, or {@code null} if no offset is set
     * (i.e. we are in the overworld coordinate frame).
     */
    public static Integer get() {
        return BAND_Y_OFFSET.get();
    }
}
