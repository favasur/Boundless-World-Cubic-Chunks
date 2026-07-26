package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen.tree;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenTaiga1.class})
public class MixinWorldGenTaiga1 {
   public MixinWorldGenTaiga1() {
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 1,
         ordinal = 2
      )}
   )
   private int getMinGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMinHeight() + 1;
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 0,
         ordinal = 1,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )}
   )
   private int getMinGenHeightCompareZero(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getMaxGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }
}
