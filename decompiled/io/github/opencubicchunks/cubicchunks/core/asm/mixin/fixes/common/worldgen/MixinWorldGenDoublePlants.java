package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDoublePlant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenDoublePlant.class})
public class MixinWorldGenDoublePlants {
   public MixinWorldGenDoublePlants() {
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 254
      )}
   )
   private int getMaxGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMaxHeight() - 2;
   }
}
