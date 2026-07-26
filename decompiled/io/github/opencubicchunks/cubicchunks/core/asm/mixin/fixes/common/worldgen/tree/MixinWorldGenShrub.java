package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen.tree;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenShrub;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenShrub.class})
public class MixinWorldGenShrub {
   public MixinWorldGenShrub() {
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO},
         ordinal = 0
      )}
   )
   private int getMinScanHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return Coords.getMinCubePopulationPos(position.func_177956_o());
   }
}
