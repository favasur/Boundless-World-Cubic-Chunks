package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({BlockChorusFlower.class})
public class MixinBlockChorusFlower {
   public MixinBlockChorusFlower() {
   }

   @ModifyConstant(
      method = {"updateTick"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int updateTick(int maxY, World worldIn, BlockPos pos, IBlockState state, Random rand) {
      return ((ICubicWorld)worldIn).getMaxHeight();
   }
}
