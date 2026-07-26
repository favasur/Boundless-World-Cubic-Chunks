package io.github.opencubicchunks.cubicchunks.core;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.CubicChunks
public final class CubicChunks {
    public static final String MODID = "cubicchunks";
    public static final int MIN_SUPPORTED_BLOCK_Y = -2147479552;
    public static final int MAX_SUPPORTED_BLOCK_Y = 2147479552;
    public static final boolean DEBUG_ENABLED = Boolean.getBoolean("cubicchunks.debug");
    public static final Logger LOGGER = LoggerFactory.getLogger("CubicChunks");

    /** Dimensions registered as cubic. CubeProviderServer only inits for these IDs. */
    private static final Set<ResourceLocation> CUBIC_DIMENSIONS = new HashSet<>();

    private CubicChunks() {
    }

    public static void registerCubicDimension(ResourceLocation dim) {
        CUBIC_DIMENSIONS.add(dim);
    }

    public static void unregisterCubicDimension(ResourceLocation dim) {
        CUBIC_DIMENSIONS.remove(dim);
    }

    public static boolean isCubicDimension(ResourceLocation dim) {
        return dim != null && CUBIC_DIMENSIONS.contains(dim);
    }

    public enum TicketType {
        PLAYER,
        FORCED
    }

    public static void bigWarning(String format, Object... data) {
        StringBuilder sb = new StringBuilder();
        sb.append("****************************************\n");
        sb.append("* ").append(String.format(format, data)).append("\n");
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < 10 && i < trace.length; i++) {
            sb.append("*  at ").append(trace[i].toString()).append(i == 9 ? "..." : "").append("\n");
        }
        sb.append("****************************************");
        LOGGER.warn(sb.toString());
    }
}
