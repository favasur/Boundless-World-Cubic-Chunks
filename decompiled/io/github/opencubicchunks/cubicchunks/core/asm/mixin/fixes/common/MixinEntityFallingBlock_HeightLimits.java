package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({EntityFallingBlock.class})
public abstract class MixinEntityFallingBlock_HeightLimits extends Entity {
   public MixinEntityFallingBlock_HeightLimits(World worldIn) {
      super(worldIn);
   }

   @ModifyConstant(
      method = {"onUpdate"},
      constant = {@Constant(
         intValue = 1
      )},
      slice = {@Slice(
         from = @At(
            value = "CONSTANT:ONE",
            args = {"intValue=100"}
         ),
         to = @At(
            value = "CONSTANT:FIRST",
            args = {"stringValue=doEntityDrops"}
         )
      )}
   )
   @Group(
      name = "onUpdateGetMinHeight",
      min = 1,
      max = 1
   )
   private int onUpdateGetMinHeight(int orig) {
      return ((ICubicWorld)this.field_70170_p).getMinHeight();
   }

   @ModifyConstant(
      method = {"onUpdate"},
      constant = {@Constant(
         intValue = 256
      )},
      slice = {@Slice(
         from = @At(
            value = "CONSTANT:ONE",
            args = {"intValue=100"}
         ),
         to = @At(
            value = "CONSTANT:LAST",
            args = {"stringValue=doEntityDrops"}
         )
      )}
   )
   @Group(
      name = "onUpdateGetMaxHeight",
      min = 1,
      max = 1
   )
   private int onUpdateGetMaxHeight(int orig) {
      return ((ICubicWorld)this.field_70170_p).getMaxHeight();
   }
}
