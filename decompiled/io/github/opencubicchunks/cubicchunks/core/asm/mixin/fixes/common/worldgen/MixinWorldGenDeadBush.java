package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenDeadBush.class})
public class MixinWorldGenDeadBush {
   private int minPos;

   public MixinWorldGenDeadBush() {
   }

   @Inject(
      method = {"generate"},
      at = {@At("HEAD")}
   )
   private void onGenerate(World worldIn, Random rand, BlockPos position, CallbackInfoReturnable<Boolean> cbi) {
      this.minPos = Coords.getMinCubePopulationPos(position.func_177956_o());
   }

   @ModifyConstant(
      method = {"generate"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_ZERO},
         ordinal = 0
      )}
   )
   @Nullable
   private int getReplaceMaterial_HeightCheckHack(int orig, World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).isCubicWorld() ? orig : this.minPos;
   }
}
