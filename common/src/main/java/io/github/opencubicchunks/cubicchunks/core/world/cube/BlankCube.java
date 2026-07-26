package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.BlankEntityContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Empty-cube sentinel. 1.21 port: CubePos constructor is now private, so we
 * build the {@code (0,0,0)} coordinate through {@link CubePos#of}. We also
 * drop the old abstract anonymous-class trick on
 * {@code LightingManager.CubeLightUpdateInfo} and let the inherited constructor
 * accept the null level + null column combination — the resulting instance is
 * a guaranteed no-op for {@code BlankCube}.
 */
public class BlankCube extends Cube {

    public BlankCube(LevelChunk column) {
        this(column, 0, 0, 0);
    }

    public BlankCube(LevelChunk column, int cubeX, int cubeY, int cubeZ) {
        super(
                new TicketList(null),
                column.getLevel(),
                column,
                CubePos.of(cubeX, cubeY, cubeZ),
                Cube.NULL_STORAGE,
                new BlankEntityContainer(),
                new HashMap<>(),
                new ConcurrentLinkedQueue<>(),
                null
        );
    }

    @Override public boolean isEmpty() { return true; }
    @Override public boolean containsBlockPos(BlockPos pos) { return false; }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public void onLoad() {}
    @Override public void onUnload() {}
    @Override public boolean needsSaving() { return false; }
    @Override public void markSaved() {}
    @Override public int getLightFor(LightLayer lightType, BlockPos pos) { return lightType == LightLayer.SKY ? 15 : 0; }
    @Override public void markForRenderUpdate() {}
}
