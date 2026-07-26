package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ICubicWorld extends IMinMaxHeight {
   boolean isCubicWorld();

   ICubeProvider getCubeCache();

   default BlockPos getSurfaceForCube(CubePos cubePos, int xOffset, int zOffset, int forcedAdditionalCubes, ICubicWorld.SurfaceType type) {
      return this.getSurfaceForCube(cubePos, xOffset, zOffset, forcedAdditionalCubes, (pos, state) -> this.canBeTopBlock(pos, state, type));
   }

   @Nullable
   default BlockPos getSurfaceForCube(CubePos pos, int xOffset, int zOffset, int forcedAdditionalCubes, BiPredicate<BlockPos, IBlockState> canBeTopBlock) {
      int maxFreeY = pos.getMaxBlockY() + 8;
      int minFreeY = pos.getMinBlockY() + 8;
      int startY = pos.above().getMaxBlockY() + forcedAdditionalCubes * 16;
      BlockPos start = new BlockPos(pos.getMinBlockX() + xOffset, startY, pos.getMinBlockZ() + zOffset);
      return this.findTopBlock(start, minFreeY, maxFreeY, canBeTopBlock);
   }

   @Nullable
   default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, ICubicWorld.SurfaceType type) {
      return this.findTopBlock(start, minTopY, maxTopY, (pos, state) -> this.canBeTopBlock(pos, state, type));
   }

   @Nullable
   default BlockPos findTopBlock(BlockPos start, int minTopY, int maxTopY, BiPredicate<BlockPos, IBlockState> canBeTopBlock) {
      BlockPos pos = start;
      IBlockState startState = ((World)this).func_180495_p(start);
      if (canBeTopBlock.test(start, startState)) {
         return null;
      } else {
         ICube cube = this.getCubeFromBlockCoords(start.func_177977_b());

         while (pos.func_177956_o() >= minTopY) {
            BlockPos next = pos.func_177977_b();
            if (Coords.blockToCube(next.func_177956_o()) != cube.getY()) {
               cube = this.getCubeFromBlockCoords(next);
            }

            if (!cube.isEmpty()) {
               IBlockState state = cube.getBlockState(next);
               if (canBeTopBlock.test(next, state)) {
                  break;
               }
            }

            pos = next;
         }

         return pos.func_177956_o() >= minTopY && pos.func_177956_o() <= maxTopY ? pos : null;
      }
   }

   default boolean canBeTopBlock(BlockPos pos, IBlockState state, ICubicWorld.SurfaceType type) {
      switch (type) {
         case SOLID:
            return state.func_185904_a().func_76230_c()
               && !state.func_177230_c().isLeaves(state, (World)this, pos)
               && !state.func_177230_c().isFoliage((World)this, pos);
         case OPAQUE:
            return state.getLightOpacity((World)this, pos) != 0;
         case BLOCKING_MOVEMENT:
            return state.func_185904_a().func_76230_c() || state.func_185904_a().func_76224_d();
         default:
            throw new IllegalArgumentException(type.toString());
      }
   }

   default boolean testForCubes(BlockPos centerPos, int blockRadius, Predicate<ICube> test) {
      return this.testForCubes(
         centerPos.func_177958_n() - blockRadius,
         centerPos.func_177956_o() - blockRadius,
         centerPos.func_177952_p() - blockRadius,
         centerPos.func_177958_n() + blockRadius,
         centerPos.func_177956_o() + blockRadius,
         centerPos.func_177952_p() + blockRadius,
         test
      );
   }

   default boolean testForCubes(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ, Predicate<ICube> test) {
      return this.testForCubes(CubePos.fromBlockCoords(minBlockX, minBlockY, minBlockZ), CubePos.fromBlockCoords(maxBlockX, maxBlockY, maxBlockZ), test);
   }

   boolean testForCubes(CubePos var1, CubePos var2, Predicate<? super ICube> var3);

   int getActualHeight();

   ICube getCubeFromCubeCoords(int var1, int var2, int var3);

   default ICube getCubeFromCubeCoords(CubePos pos) {
      return this.getCubeFromCubeCoords(pos.getX(), pos.getY(), pos.getZ());
   }

   ICube getCubeFromBlockCoords(BlockPos var1);

   int getEffectiveHeight(int var1, int var2);

   boolean isBlockColumnLoaded(BlockPos var1);

   boolean isBlockColumnLoaded(BlockPos var1, boolean var2);

   int getMinGenerationHeight();

   int getMaxGenerationHeight();

   public static enum SurfaceType {
      SOLID,
      BLOCKING_MOVEMENT,
      OPAQUE;

      private SurfaceType() {
      }
   }
}
