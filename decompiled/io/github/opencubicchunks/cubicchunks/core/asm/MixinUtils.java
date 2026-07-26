package io.github.opencubicchunks.cubicchunks.core.asm;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MixinUtils {
   public MixinUtils() {
   }

   public static boolean canTickPosition(World world, BlockPos pos) {
      return canTickPosition(world, pos, null);
   }

   public static boolean canTickPosition(World world, BlockPos pos, @Nullable Predicate<Cube> canTickCube) {
      if (!world.func_175701_a(pos)) {
         return true;
      } else if (!world.func_175667_e(pos)) {
         return false;
      } else {
         return canTickCube == null ? true : canTickCube.test(((ICubicWorldInternal)world).getCubeFromBlockCoords(pos));
      }
   }
}
