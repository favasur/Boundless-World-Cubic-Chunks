package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.cube.StubCubeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.function.Predicate;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld
@Mixin(Level.class)
public abstract class MixinLevel implements ICubicWorldInternal, LevelHeightAccessor {

    @Shadow
    public abstract DimensionType dimensionType();

    protected boolean isCubicWorld;
    @Nullable
    protected LightingManager lightingManager;
    @Nullable
    protected ICubeProvider cc$cubeCache;

    @Override
    public void initCubicWorld() {
        this.isCubicWorld = true;
        this.lightingManager = new LightingManager((Level) (Object) this);
        this.cc$cubeCache = new StubCubeProvider((Level) (Object) this);
    }

    @Override
    public void initCubicWorldClient() {
        initCubicWorld();
    }

    @Override
    public boolean isCubicWorld() {
        return this.isCubicWorld;
    }

    @Override
    public int getMinHeight() {
        return this.dimensionType().minY();
    }

    @Override
    public int getMaxHeight() {
        return this.dimensionType().minY() + this.dimensionType().height();
    }

    @Override
    public int getMinGenerationHeight() {
        return this.dimensionType().minY();
    }

    @Override
    public int getMaxGenerationHeight() {
        return this.dimensionType().minY() + this.dimensionType().height();
    }

    @Override
    public void fakeWorldHeight(int height) {
        // In an unbounded cubic world there is no single fake height to report.
    }

    // Keep LevelHeightAccessor values at vanilla-compatible bounds so vanilla arrays
    // (chunk sections, light storage, heightmaps) do not explode. Real cubic block
    // access is routed through the cube cache, and isOutsideBuildHeight always
    // returns false so players can build/destroy blocks at any Y.
    public int getMinBuildHeight() {
        return this.dimensionType().minY();
    }

    public int getHeight() {
        return this.dimensionType().height();
    }

    @Override
    public int getMaxBuildHeight() {
        return this.dimensionType().minY() + this.dimensionType().height();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return !this.isCubicWorld;
    }

    @Override
    public LightingManager getLightingManager() {
        if (!this.isCubicWorld) {
            throw new IllegalStateException("Not a cubic world");
        }
        if (this.lightingManager == null) {
            throw new IllegalStateException("LightingManager not initialized");
        }
        return this.lightingManager;
    }

    @Override
    public void tickCubicWorld() {
        if (this.lightingManager != null) {
            this.lightingManager.onTick();
        }
    }

    @Override
    public Level getLevel() {
        return (Level) (Object) this;
    }

    @Override
    public ICubeProvider getCubeCache() {
        if (this.cc$cubeCache == null) {
            this.cc$cubeCache = new StubCubeProvider((Level) (Object) this);
        }
        return this.cc$cubeCache;
    }

    @Override
    public void setCubeCache(ICubeProvider provider) {
        this.cc$cubeCache = provider;
    }

    @Inject(
            method = "getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cc$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!this.isCubicWorld) {
            return;
        }
        ICube cube = this.getCubeCache().getLoadedCube(
                Coords.blockToCube(pos.getX()),
                Coords.blockToCube(pos.getY()),
                Coords.blockToCube(pos.getZ())
        );
        if (cube != null) {
            cir.setReturnValue(cube.getBlockState(pos));
        } else {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    public int getActualHeight() {
        return this.getMaxHeight() - this.getMinHeight();
    }

    @Override
    public ICube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ) {
        ICubeProvider cache = this.getCubeCache();
        return cache != null ? cache.getCube(cubeX, cubeY, cubeZ) : null;
    }

    @Override
    public ICube getCubeFromBlockCoords(BlockPos pos) {
        return this.getCubeFromCubeCoords(
                Coords.blockToCube(pos.getX()),
                Coords.blockToCube(pos.getY()),
                Coords.blockToCube(pos.getZ())
        );
    }

    @Override
    public int getEffectiveHeight(int x, int z) {
        return 0;
    }

    @Override
    public boolean isBlockColumnLoaded(BlockPos pos) {
        return false;
    }

    @Override
    public boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty) {
        return false;
    }

    @Override
    public boolean testForCubes(CubePos start, CubePos end, Predicate<? super ICube> cubeAllowed) {
        return false;
    }
}
