package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({Entity.class})
public class MixinEntity_PrepareSpawn {
   @Shadow
   public World field_70170_p;

   public MixinEntity_PrepareSpawn() {
   }

   @ModifyConstant(
      method = {"preparePlayerToSpawn"},
      constant = {@Constant(
         doubleValue = 0.0
      )}
   )
   private double getMinHeight(double zero) {
      return !this.field_70170_p.func_175667_e(new BlockPos((Entity)this))
         ? Double.POSITIVE_INFINITY
         : (double)((ICubicWorld)this.field_70170_p).getMinHeight();
   }

   @ModifyConstant(
      method = {"preparePlayerToSpawn"},
      constant = {@Constant(
         doubleValue = 256.0
      )}
   )
   private double getMaxHeight(double _256) {
      return (double)((ICubicWorld)this.field_70170_p).getMaxHeight();
   }
}
