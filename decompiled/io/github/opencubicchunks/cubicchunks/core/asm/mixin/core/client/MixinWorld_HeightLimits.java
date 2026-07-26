package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({World.class})
public abstract class MixinWorld_HeightLimits implements ICubicWorld {
   @Final
   @Shadow
   public WorldProvider field_73011_w;

   public MixinWorld_HeightLimits() {
   }

   @Shadow
   public abstract boolean func_175701_a(BlockPos var1);

   @Shadow
   public abstract boolean func_175667_e(BlockPos var1);

   @Shadow
   public abstract IBlockState func_180495_p(BlockPos var1);

   @Shadow
   public abstract int func_175642_b(EnumSkyBlock var1, BlockPos var2);

   @ModifyConstant(
      method = {"getLightFromNeighborsFor"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO}
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;getY()I"
         ),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;getZ()I"
         )
      )}
   )
   @Group(
      name = "getLightFromNeighborsFor",
      min = 2,
      max = 3
   )
   private int getLightFromNeighborsFor_getMinHeight(int zero) {
      return this.getMinHeight();
   }

   @ModifyArg(
      method = {"getLightFromNeighborsFor"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/BlockPos;<init>(III)V"
      ),
      index = 1,
      require = 1
   )
   @Group(
      name = "getLightFromNeighborsFor"
   )
   private int getLightFromNeighborsForGetMinHeight(int origY) {
      return this.getMinHeight();
   }
}
