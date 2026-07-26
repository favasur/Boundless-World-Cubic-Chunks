package io.github.opencubicchunks.cubicchunks.core.debug;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.debug.Dbg
// 1.21: thin facade for /cubicchunks debug output.
public final class Dbg {

    private Dbg() {
    }

    public static String cubeSummary(CubePos pos) {
        return String.format("cube[%d,%d,%d]/[0x%s]", pos.getX(), pos.getY(), pos.getZ(),
                Integer.toHexString(pos.hashCode()));
    }

    public static void dump(String label, Object obj) {
        if (!CubicChunks.DEBUG_ENABLED) return;
        CubicChunks.LOGGER.debug("Dbg.{} {}\n{}", label, System.identityHashCode(obj), obj);
    }
}
