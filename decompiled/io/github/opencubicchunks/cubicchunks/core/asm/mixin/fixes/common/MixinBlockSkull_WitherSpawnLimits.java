package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockSkull;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({BlockSkull.class})
public class MixinBlockSkull_WitherSpawnLimits {
   public MixinBlockSkull_WitherSpawnLimits() {
   }

   @ModifyConstant(
      method = {"checkWitherSpawn"},
      constant = {@Constant(
         intValue = 2,
         ordinal = 0
      )}
   )
   private int checkWitherSpawnHeightLimit(int originalHeight, World worldIn, BlockPos pos, TileEntitySkull te) {
      return ((ICubicWorld)worldIn).getMinHeight() + originalHeight;
   }
}
