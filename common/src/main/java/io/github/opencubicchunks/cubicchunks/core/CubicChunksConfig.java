package io.github.opencubicchunks.cubicchunks.core;

import io.github.opencubicchunks.cubicchunks.core.lighting.LightingMode;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig
public final class CubicChunksConfig {
    public static int chunkGCInterval = 200;
    public static boolean optimizedCompatibilityGenerator = true;
    public static int maxGeneratedCubesPerTick = 784;
    public static int maxCubeGenerationTimeMillis = 50;
    public static boolean useVanillaChunkWorldGenerators = false;
    public static int verticalCubeLoadDistance = 8;
    public static String[] excludedDimensions = new String[]{"1"};
    public static boolean forceDimensionExcludes = false;
    public static int relightChecksPerTickPerColumn = 1;
    public static boolean doClientLightFixes = false;
    public static int biomeTemperatureCenterY = 64;
    public static float biomeTemperatureHeightFactor = -0.0016666667F;
    public static int biomeTemperatureScaleMaxY = 256;
    public static String compatibilityGeneratorType = "cubicchunks:default";
    public static String storageFormat = "";
    public static int spawnGenerateDistanceXZ = 12;
    public static int spawnGenerateDistanceY = 8;
    public static int spawnLoadDistanceXZ = 8;
    public static int spawnLoadDistanceY = 8;
    public static int defaultMinHeight = -1073741824;
    public static int defaultMaxHeight = 1073741824;
    public static boolean replaceLightRecheck = false;
    public static boolean updateKnownBrokenLightingOnLoad = true;
    public static int worldgenWatchdogTimeLimit = 10000;
    public static boolean allowVanillaClients = false;
    public static boolean fastSimplifiedSkyLight = false;
    public static int cubesToSendPerTick = 649;
    public static boolean useShadowPagingIO = true;
    public static int defaultMaxCubesPerChunkloadingTicket = 400;
    /**
     * When true, the overworld's cube provider dispatches generation by cube Y to a
     * stacked sub-dim band in addition to the overworld Y window. Stacked sub-dims:
     * Nether at [{@code -160, -16}] and End at [{@code 400, 720}]. Vanilla 'Nether'
     * and 'End' ServerLevels are absorbed into the overworld's save file.
     */
    public static boolean stackingDimensionsEnabled = true;

    /**
     * Lighting dispatch mode for {@code LightingManager.onTick()}. Default
     * {@link io.github.opencubicchunks.cubicchunks.core.lighting.LightingMode#SYNC}
     * preserves existing behaviour one-for-one. Switching to
     * {@link io.github.opencubicchunks.cubicchunks.core.lighting.LightingMode#ASYNC_BATCHED}
     * fans out each {@code CubeLightUpdateInfo.tick()} to
     * {@code Util.backgroundExecutor()} and joins on
     * {@code CompletableFuture.allOf(...)}. This is purely additive — no other
     * code reads this field and the default keeps the original sequential path.
     *
     * <p>Override at JVM launch with {@code -Dcubicchunks.lightingMode=ASYNC_BATCHED}.
     */
    public static LightingMode lightingMode = LightingMode.SYNC;

    static {
        // System-property override: -Dcubicchunks.lightingMode=ASYNC_BATCHED
        String prop = System.getProperty("cubicchunks.lightingMode");
        if (prop != null && !prop.isEmpty()) {
            try {
                lightingMode = LightingMode.valueOf(prop);
            } catch (IllegalArgumentException ignored) {
                // Falls through to default; bad value should never crash a load.
            }
        }
    }

    private CubicChunksConfig() {
    }
}
