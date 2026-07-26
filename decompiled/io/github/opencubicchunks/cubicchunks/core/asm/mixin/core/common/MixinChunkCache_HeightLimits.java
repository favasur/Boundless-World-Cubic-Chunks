package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ChunkCache.class})
public class MixinChunkCache_HeightLimits {
   @Shadow
   public World field_72815_e;

   public MixinChunkCache_HeightLimits() {
   }

   @ModifyConstant(
      method = {"getBlockState"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO},
         ordinal = 0
      )}
   )
   private int getBlockState_getMinHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMinHeight();
   }

   @ModifyConstant(
      method = {"getBlockState"},
      constant = {@Constant(
         intValue = 256
      )}
   )
   private int getBlockState_getMaxHeight(int orig) {
      return ((ICubicWorld)this.field_72815_e).getMaxHeight();
   }

   public int getMinHeight() {
      return ((ICubicWorld)this.field_72815_e).getMinHeight();
   }

   public int getMaxHeight() {
      return ((ICubicWorld)this.field_72815_e).getMaxHeight();
   }
}
