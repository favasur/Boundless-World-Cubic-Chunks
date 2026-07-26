package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPortal.Size;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({Size.class})
public abstract class MixinBlockPortal_Size_HeightLimits {
   public MixinBlockPortal_Size_HeightLimits() {
   }

   @ModifyConstant(
      method = {"<init>"},
      constant = {@Constant(
         intValue = 0,
         ordinal = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO}
      )},
      require = 1
   )
   private int portalSizeClassInitReplace0(int posY, World worldIn, BlockPos origin, Axis axis) {
      return ((ICubicWorld)worldIn).getMinHeight();
   }
}
