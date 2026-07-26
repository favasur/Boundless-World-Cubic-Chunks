package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenLakes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenLakes.class})
public class MixinWorldGenLakes {
   private int minY;

   public MixinWorldGenLakes() {
   }

   @Inject(
      method = {"generate"},
      at = {@At("HEAD")}
   )
   private void onGenerate(World worldIn, Random rand, BlockPos position, CallbackInfoReturnable<Boolean> cbi) {
      this.minY = Coords.getMinCubePopulationPos(position.func_177956_o());
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 5,
         ordinal = 0
      )}
   )
   private int getMinGenHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).isCubicWorld() ? this.minY : orig;
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 4,
         ordinal = 0
      )}
   )
   private int getMinWorldHeight(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).getMinHeight() + orig;
   }
}
