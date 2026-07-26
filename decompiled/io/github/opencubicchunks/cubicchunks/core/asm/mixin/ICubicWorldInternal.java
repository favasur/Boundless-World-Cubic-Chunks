package io.github.opencubicchunks.cubicchunks.core.asm.mixin;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.FirstLightProcessor;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickList;
import io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickSet;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicWorldInternal extends ICubicWorld {
   void tickCubicWorld();

   ICubeProviderInternal getCubeCache();

   LightingManager getLightingManager();

   Cube getCubeFromCubeCoords(int var1, int var2, int var3);

   Cube getCubeFromBlockCoords(BlockPos var1);

   void fakeWorldHeight(int var1);

   default BlockPos getTopSolidOrLiquidBlockVanilla(BlockPos pos) {
      Chunk chunk = ((World)this).func_175726_f(pos);
      BlockPos current = new BlockPos(pos.func_177958_n(), chunk.func_76625_h() + 16, pos.func_177952_p());

      while (current.func_177956_o() >= 0) {
         BlockPos next = current.func_177977_b();
         IBlockState state = chunk.func_177435_g(next);
         if (state.func_185904_a().func_76230_c()
            && !state.func_177230_c().isLeaves(state, (World)this, next)
            && !state.func_177230_c().isFoliage((World)this, next)) {
            break;
         }

         current = next;
      }

      return current;
   }

   public interface Client extends ICubicWorldInternal {
      void initCubicWorldClient(IntRange var1, IntRange var2);

      CubeProviderClient getCubeCache();

      void setHeightBounds(int var1, int var2);
   }

   public interface CompatGenerationScope extends AutoCloseable {
      @Override
      void close();
   }

   public interface Server extends ICubicWorldInternal, ICubicWorldServer {
      void initCubicWorldServer(IntRange var1, IntRange var2);

      CubeProviderServer getCubeCache();

      FirstLightProcessor getFirstLightProcessor();

      void removeForcedCube(ICube var1);

      void addForcedCube(ICube var1);

      XYZMap<ICube> getForcedCubes();

      XZMap<IColumn> getForcedColumns();

      CubeSplitTickSet getScheduledTicks();

      CubeSplitTickList getThisTickScheduledTicks();

      SpawnCubes getSpawnArea();

      void setSpawnArea(SpawnCubes var1);

      ICubicWorldInternal.CompatGenerationScope doCompatibilityGeneration();

      boolean isCompatGenerationScope();

      VanillaNetworkHandler getVanillaNetworkHandler();
   }
}
