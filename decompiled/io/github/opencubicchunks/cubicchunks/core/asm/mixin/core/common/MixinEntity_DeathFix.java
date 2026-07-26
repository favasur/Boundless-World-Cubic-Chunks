package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({Entity.class})
public class MixinEntity_DeathFix {
   @Shadow
   public World field_70170_p;

   public MixinEntity_DeathFix() {
   }

   @ModifyConstant(
      method = {"onEntityUpdate"},
      constant = {@Constant(
         doubleValue = -64.0
      )},
      require = 1
   )
   private double getDeathY(double originalY) {
      return (double)((ICubicWorld)this.field_70170_p).getMinHeight() + originalY;
   }
}
