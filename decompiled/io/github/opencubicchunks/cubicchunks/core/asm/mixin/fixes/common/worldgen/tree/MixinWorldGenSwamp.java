package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen.tree;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenSwamp.class})
public class MixinWorldGenSwamp {
   private int minPos;

   public MixinWorldGenSwamp() {
   }

   @Inject(
      method = {"generate"},
      at = {@At("HEAD")}
   )
   private void onGenerate(World worldIn, Random rand, BlockPos position, CallbackInfoReturnable<Boolean> cbi) {
      this.minPos = Coords.getMinCubePopulationPos(position.func_177956_o());
   }

   @Redirect(
      method = {"generate"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/block/material/Material;WATER:Lnet/minecraft/block/material/Material;",
         ordinal = 0
      )
   )
   @Nullable
   private Material getReplaceMaterial_HeightCheckHack(World worldIn, Random rand, BlockPos position) {
      return ((ICubicWorld)worldIn).isCubicWorld() && position.func_177956_o() < this.minPos ? null : Material.field_151586_h;
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
