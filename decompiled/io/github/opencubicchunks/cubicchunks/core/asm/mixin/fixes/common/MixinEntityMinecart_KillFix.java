package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({EntityMinecart.class})
public abstract class MixinEntityMinecart_KillFix extends Entity {
   public MixinEntityMinecart_KillFix(World worldIn) {
      super(worldIn);
   }

   @ModifyConstant(
      method = {"onUpdate"},
      constant = {@Constant(
         doubleValue = -64.0
      )},
      require = 1
   )
   private double getDeathY(double originalY) {
      return (double)((ICubicWorld)this.field_70170_p).getMinHeight() + originalY;
   }
}
