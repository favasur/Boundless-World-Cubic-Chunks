package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.DefaultCubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ServerCubeIO;
import io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedCubeGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorldServer
// 1.21: blocks every dimension from being cubic unless the dimension registry key is in
// CubicChunks.CUBIC_DIMENSIONS. When {@link CubicChunksConfig#stackingDimensionsEnabled}
// is true, the overworld is given {@link StackedCubeGenerator} instead of the
// default — this absorbs vanilla 'Nether' / 'End' ServerLevels into Y-bands inside
// the overworld's single save file.
@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {

    @Inject(method = "close()V", at = @At("RETURN"))
    private void cc$close(CallbackInfo ci) {
        ICubeProvider provider = ((ICubicWorldInternal) this).getCubeCache();
        if (provider != null) {
            provider.close();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cc$init(CallbackInfo ci) {
        ServerLevel serverLevel = (ServerLevel) (Object) this;
        ResourceLocation dimKey = serverLevel.dimension().location();
        if (!CubicChunks.isCubicDimension(dimKey)) {
            return; // vanilla world; remain bounded, do not init cubic provider
        }
        ICubicWorldInternal world = (ICubicWorldInternal) this;
        world.initCubicWorld();
        ICubeGenerator gen = selectGenerator(serverLevel, dimKey);
        ServerCubeIO cubeIO = new ServerCubeIO(serverLevel);
        CubeProviderServer cubeCache = new CubeProviderServer(serverLevel, gen, cubeIO);
        world.setCubeCache(cubeCache);
        if (gen instanceof StackedCubeGenerator stacked) {
            CubicChunks.LOGGER.info("Initialised cubic world for dimension {} (stacked: {} sub-dims)",
                    dimKey, StackedDimensionRegistry.size());
        } else {
            CubicChunks.LOGGER.info("Initialised cubic world for dimension {}", dimKey);
        }
    }

    /**
     * Picks the right generator for the active ServerLevel. If stacking is enabled
     * AND the dimension key matches the overworld, returns a {@link StackedCubeGenerator}
     * wrapping the vanilla column. Otherwise returns a regular {@link DefaultCubeGenerator}.
     * Always prefers the overworld being "stacked" above all other dims so that the
     * {@code minecraft:overworld} ServerLevel is the one absorbing the nether / end bands.
     */
    private static ICubeGenerator selectGenerator(ServerLevel serverLevel, ResourceLocation dimKey) {
        boolean isOverworld = dimKey.getNamespace().equals("minecraft") && dimKey.getPath().equals("overworld");
        if (CubicChunksConfig.stackingDimensionsEnabled && isOverworld) {
            CubicChunks.LOGGER.info("Stacking dimensions ENABLED: overworld {} will host Nether=[{},{}] and End=[{},{}]",
                    StackedDimensions.OVERWORLD_ID,
                    StackedDimensions.NETHER.minBlockY(), StackedDimensions.NETHER.maxBlockY(),
                    StackedDimensions.END.minBlockY(), StackedDimensions.END.maxBlockY());
            return new StackedCubeGenerator(serverLevel);
        }
        return new DefaultCubeGenerator(serverLevel);
    }
}

