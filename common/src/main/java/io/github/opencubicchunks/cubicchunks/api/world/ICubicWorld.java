package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Implemented by worlds/dimensions that use cubic chunks.
 */
public interface ICubicWorld extends IMinMaxHeight {
    boolean isCubicWorld();

    ICubeProvider getCubeCache();

    default BlockPos getSurfaceForCube(CubePos cubePos, int xOffset, int zOffset, int forcedAdditionalCubes, SurfaceType type) {
        return getSurfaceForCube(cubePos, xOffset, zOffset, forcedAdditionalCubes, (pos, state) -> canBeTopBlock(pos, state, type));
    }

    @Nullable
    default BlockPos getSurfaceForCube(CubePos cubePos, int xOffset, int zOffset, int forcedAdditionalCubes, BiPredicate<BlockPos, BlockState> canBeTopBlock) {
        int minFreeY = cubePos.getMinBlockY() + 8;
        int maxFreeY = cubePos.getMaxBlockY() + 8;
        int startY = cubePos.above().getMaxBlockY() + forcedAdditionalCubes * 16;
        BlockPos start = new BlockPos(cubePos.getMinBlockX() + xOffset, startY, cubePos.getMinBlockZ() + zOffset);
        return findTopBlock(start, minFreeY, maxFreeY, canBeTopBlock);
    }

    @Nullable
    default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, SurfaceType type) {
        return findTopBlock(start, minTopY, maxTopY, (pos, state) -> canBeTopBlock(pos, state, type));
    }

    Level getLevel();

    @Nullable
    default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, BiPredicate<BlockPos, BlockState> canBeTopBlock) {
        Level level = getLevel();
        BlockPos pos = start;
        BlockState startState = level.getBlockState(start);
        if (canBeTopBlock.test(start, startState)) {
            return null;
        }

        ICube cube = getCubeFromBlockCoords(start.below());
        while (pos.getY() >= minTopY) {
            BlockPos next = pos.below();
            if (CubePos.fromBlockCoords(next.getX(), next.getY(), next.getZ()).getY() != cube.getY()) {
                cube = getCubeFromBlockCoords(next);
            }

            if (!cube.isEmpty()) {
                BlockState state = cube.getBlockState(next);
                if (canBeTopBlock.test(next, state)) {
                    break;
                }
            }

            pos = next;
        }

        return pos.getY() >= minTopY && pos.getY() <= maxTopY ? pos : null;
    }

    default boolean canBeTopBlock(BlockPos pos, BlockState state, SurfaceType type) {
        Level level = getLevel();
        return switch (type) {
            case SOLID -> state.isSolid();
            case OPAQUE -> state.getLightBlock(level, pos) != 0;
            case BLOCKING_MOVEMENT -> state.isSolid() || !state.getCollisionShape(level, pos).isEmpty();
        };
    }

    default boolean testForCubes(BlockPos centerPos, int blockRadius, Predicate<ICube> test) {
        return testForCubes(
            centerPos.getX() - blockRadius,
            centerPos.getY() - blockRadius,
            centerPos.getZ() - blockRadius,
            centerPos.getX() + blockRadius,
            centerPos.getY() + blockRadius,
            centerPos.getZ() + blockRadius,
            test
        );
    }

    default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, Predicate<ICube> test) {
        return testForCubes(
            CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ),
            CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ),
            test
        );
    }

    boolean testForCubes(CubePos min, CubePos max, Predicate<? super ICube> test);

    int getActualHeight();

    ICube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ);

    default ICube getCubeFromCubeCoords(CubePos pos) {
        return getCubeFromCubeCoords(pos.getX(), pos.getY(), pos.getZ());
    }

    ICube getCubeFromBlockCoords(BlockPos pos);

    int getEffectiveHeight(int x, int z);

    boolean isBlockColumnLoaded(BlockPos pos);

    boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty);

    int getMinGenerationHeight();

    int getMaxGenerationHeight();

    enum SurfaceType {
        SOLID,
        BLOCKING_MOVEMENT,
        OPAQUE
    }
}
