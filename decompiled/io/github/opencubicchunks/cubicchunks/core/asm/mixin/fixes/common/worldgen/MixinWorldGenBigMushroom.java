package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenBigMushroom.class})
public class MixinWorldGenBigMushroom {
   public MixinWorldGenBigMushroom() {
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 1,
         ordinal = 1
      )}
   )
   private int getMinGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMinHeight() + 1;
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 0,
         ordinal = 2,
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
