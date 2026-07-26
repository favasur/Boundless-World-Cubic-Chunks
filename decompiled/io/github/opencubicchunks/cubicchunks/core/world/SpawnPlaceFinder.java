package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SpawnPlaceFinder {
   private static final int MIN_FREE_SPACE_SPAWN = 32;

   private SpawnPlaceFinder() {
      throw new Error();
   }

   public static BlockPos getRandomizedSpawnPoint(World world) {
      BlockPos ret = world.func_175694_M();
      CubicChunks.LOGGER.trace("Finding spawnpoint starting from {}", ret);
      boolean isAdventure = world.func_72912_H().func_76077_q() == GameType.ADVENTURE;
      int spawnFuzz;
      if (world instanceof WorldServer) {
         spawnFuzz = world.func_175624_G().getSpawnFuzz((WorldServer)world, Objects.requireNonNull(world.func_73046_m()));
      } else {
         spawnFuzz = 1;
      }

      int border = MathHelper.func_76128_c(world.func_175723_af().func_177729_b((double)ret.func_177958_n(), (double)ret.func_177952_p()));
      if (border < spawnFuzz) {
         spawnFuzz = border;
      }

      if (!world.field_73011_w.func_177495_o() && !isAdventure && spawnFuzz != 0) {
         if (spawnFuzz < 2) {
            spawnFuzz = 2;
         }

         int spawnFuzzHalf = spawnFuzz / 2;
         CubicChunks.LOGGER.trace("Running bisect with spawn fizz {}", spawnFuzz);
         ret = getTopBlockBisect(
            world, ret.func_177982_a(world.field_73012_v.nextInt(spawnFuzzHalf) - spawnFuzz, 0, world.field_73012_v.nextInt(spawnFuzzHalf) - spawnFuzz)
         );
         if (ret == null) {
            ret = world.func_175694_M();
            CubicChunks.LOGGER.trace("No spawnpoint place found starting at {}, spawning at {}", ret, ret);
         } else {
            ret = ret.func_177984_a();
         }
      }

      return ret;
   }

   @Nullable
   public static BlockPos getTopBlockBisect(World world, BlockPos pos) {
      BlockPos minPos;
      BlockPos maxPos;
      if (findNonEmpty(world, pos) == null) {
         CubicChunks.LOGGER.trace("Starting bisect with empty space at init {}", pos);
         maxPos = pos;
         minPos = findMinPos(world, pos);
         CubicChunks.LOGGER.trace("Found minPos {} and maxPos {}", minPos, pos);
      } else {
         CubicChunks.LOGGER.trace("Starting bisect without empty space at init {}", pos);
         minPos = pos;
         maxPos = findMaxPos(world, pos);
         CubicChunks.LOGGER.trace("Found minPos {} and maxPos {}", pos, maxPos);
      }

      if (minPos != null && maxPos != null) {
         assert findNonEmpty(world, maxPos) == null && findNonEmpty(world, minPos) != null;

         return bisect(world, minPos.func_177979_c(32), maxPos.func_177981_b(32));
      } else {
         CubicChunks.LOGGER.error("No suitable spawn found, using original input {} (min={}, max={})", pos, minPos, maxPos);
         return pos;
      }
   }

   @Nullable
   private static BlockPos bisect(World world, BlockPos min, BlockPos max) {
      while (min.func_177956_o() < max.func_177956_o() - 1) {
         CubicChunks.LOGGER.trace("Bisect step with min={}, max={}", min, max);
         BlockPos middle = middleY(min, max);
         if (findNonEmpty(world, middle) != null) {
            min = middle;
         } else {
            max = middle;
         }
      }

      return findNonEmpty(world, min);
   }

   private static BlockPos middleY(BlockPos min, BlockPos max) {
      return new BlockPos(min.func_177958_n(), (int)((long)min.func_177956_o() + (long)max.func_177956_o() >> 1), min.func_177952_p());
   }

   @Nullable
   private static BlockPos findMinPos(World world, BlockPos pos) {
      double dy;
      for (dy = 16.0; findNonEmpty(world, inWorldUp(world, pos, -dy)) == null; dy *= 2.0) {
         if (dy > 2.147483647E9) {
            CubicChunks.LOGGER.trace("Error finding spawn point: can't find solid start height at {}", pos);
            return null;
         }
      }

      return inWorldUp(world, pos, -dy);
   }

   @Nullable
   private static BlockPos findMaxPos(World world, BlockPos pos) {
      double dy;
      for (dy = 16.0; findNonEmpty(world, inWorldUp(world, pos, dy)) != null; dy *= 2.0) {
         if (dy > 2.147483647E9) {
            CubicChunks.LOGGER.trace("Error finding spawn point: can't find non-solid end height at {}", pos);
            return null;
         }
      }

      return inWorldUp(world, pos, dy);
   }

   @Nullable
   private static BlockPos findNonEmpty(World world, BlockPos pos) {
      pos = pos.func_177979_c(32);

      for (int i = 0; i < 64; pos = pos.func_177984_a()) {
         ((ICubicWorldServer)world)
            .getCubeCache()
            .getCubeNow(
               Coords.blockToCube(pos.func_177958_n()),
               Coords.blockToCube(pos.func_177956_o()),
               Coords.blockToCube(pos.func_177952_p()),
               ICubeProviderServer.Requirement.POPULATE
            );
         if (world.func_180495_p(pos).isSideSolid(world, pos, EnumFacing.UP)) {
            return pos;
         }

         i++;
      }

      return null;
   }

   private static BlockPos inWorldUp(World world, BlockPos original, double up) {
      int y = (int)((double)original.func_177956_o() + up);
      y = MathHelper.func_76125_a(y, ((ICubicWorld)world).getMinHeight(), ((ICubicWorld)world).getMaxHeight());
      return new BlockPos(original.func_177958_n(), y, original.func_177952_p());
   }
}
