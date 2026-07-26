package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityLockable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({TileEntityBeacon.class})
public abstract class MixinTileEntityBeaconBetterFps extends TileEntityLockable {
   public MixinTileEntityBeaconBetterFps() {
   }

   @ModifyConstant(
      method = {"updateLevels(III)V"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO}
      )},
      remap = false
   )
   private int updateLevelsYValue(int orig) {
      return ((ICubicWorld)this.field_145850_b).getMinHeight();
   }
}
