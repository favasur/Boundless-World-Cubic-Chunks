package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * Sentinel column for positions where no real cube-bearing LevelChunk has been produced.
 * Carries the empty {@link BlankCube} lookup so {@code CubeProviderServer} never returns null.
 * 1.21 port: extend LevelChunk so all abstract ChunkAccess methods are inherited natively.
 */
public class EmptyColumn extends LevelChunk implements IColumn, IColumnInternal {
    private static final LevelChunkSection[] EMPTY_SECTIONS = new LevelChunkSection[0];

    @Override public int getX() { return this.chunkPos.x; }
    @Override public int getZ() { return this.chunkPos.z; }
    @Override public net.minecraft.world.level.Level getWorld() { return this.getLevel(); }

    private final ICube emptyCube;

    public EmptyColumn(ServerLevel world, int x, int z) {
        // 1.21.x: LevelChunkTicks moved to net.minecraft.world.ticks (not chunk).
        // The LevelChunk 9-arg ctor takes typed LevelChunkTicks<Block> + LevelChunkTicks<Fluid>;
        // call super() first with fresh empty tick containers, then init our state.
        super(world, new net.minecraft.world.level.ChunkPos(x, z),
                net.minecraft.world.level.chunk.UpgradeData.EMPTY,
                new net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.block.Block>(),
                new net.minecraft.world.ticks.LevelChunkTicks<net.minecraft.world.level.material.Fluid>(),
                0L,
                EMPTY_SECTIONS,
                null,
                null);
        this.emptyCube = new BlankCube(this);
    }

    @Override public int getHeight(BlockPos pos) { return 0; }
    @Override public int getHeightValue(int localX, int blockY, int localZ) { return 0; }
    @Override public boolean shouldTick() { return false; }
    @Override public Collection<? extends ICube> getLoadedCubes() { return Collections.emptyList(); }
    @Override public Iterable<? extends ICube> getLoadedCubes(int startY, int endY) { return Collections.emptyList(); }
    @Nullable @Override public ICube getLoadedCube(int cubeY) { return null; }
    @Override public ICube getCube(int cubeY) { return this.emptyCube; }
    @Override public void addCube(ICube cube) { throw new RuntimeException("EmptyColumn.addCube"); }
    @Nullable @Override public ICube removeCube(int cubeY) { return null; }
    @Override public boolean hasLoadedCubes() { return false; }
    @Override public void preCacheCube(ICube cube) { }
    @Override public CubePrimer getCompatGenerationPrimer() { return null; }
    @Override public void removeFromStagingHeightmap(ICube cube) { }
    @Override public void addToStagingHeightmap(ICube cube) { }
    @Override public int getHeightWithStaging(int localX, int localZ) { return 0; }
    @Override public IHeightMap getOpacityIndex() { return null; }
    @Override public BlockState getBlockState(BlockPos pos) {
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
    @Override public int getLightEmission(BlockPos pos) { return 0; }
    

    /** Helper for legacy code paths that need an instance from a server level. */
    public static EmptyColumn create(ServerLevel world, int x, int z) {
        return new EmptyColumn(world, x, z);
    }
}
