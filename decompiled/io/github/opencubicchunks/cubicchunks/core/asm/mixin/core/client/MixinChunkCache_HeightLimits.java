package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ChunkCache.class})
public abstract class MixinChunkCache_HeightLimits {
   @Shadow
   public World field_72815_e;

   public MixinChunkCache_HeightLimits() {
   }

   @ModifyConstant(
      method = {"getLightFor"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )}
   )
   private int getLightFor_getMinHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMinHeight();
   }

   @ModifyConstant(
      method = {"getLightFor"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getLightFor_getMaxHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMaxHeight();
   }

   @ModifyConstant(
      method = {"getLightForExt"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE:FIRST",
            target = "Lnet/minecraft/util/math/BlockPos;getY()I"
         ),
         to = @At(
            value = "INVOKE:FIRST",
            target = "Lnet/minecraft/world/ChunkCache;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
         )
      )}
   )
   private int getLightForExt_getMinHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMinHeight();
   }

   @ModifyConstant(
      method = {"getLightForExt"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getLightForExt_getMaxHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMaxHeight();
   }
}
