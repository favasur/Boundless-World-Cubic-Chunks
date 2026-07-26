package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemChorusFruit;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ItemChorusFruit.class})
public abstract class MixinItemChorusFruit {
   public MixinItemChorusFruit() {
   }

   @ModifyConstant(
      method = {"onItemUseFinish"},
      constant = {@Constant(
         doubleValue = 0.0
      )}
   )
   private double getMinHeight(double orig, ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
      return (double)((ICubicWorld)worldIn).getMinHeight();
   }

   @Redirect(
      method = {"onItemUseFinish"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getActualHeight()I"
      )
   )
   private int getMaxHeight(World world) {
      return ((ICubicWorld)world).isCubicWorld() ? world.func_72800_K() : world.func_72940_L();
   }
}
