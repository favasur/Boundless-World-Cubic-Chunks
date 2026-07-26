package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketMoveVehicle.class})
public class MixinSPacketMoveVehicle implements IPositionPacket {
   @Shadow
   private double field_186960_a;
   @Shadow
   private double field_186961_b;
   @Shadow
   private double field_186962_c;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketMoveVehicle() {
   }

   @Override
   public void setPosOffset(BlockPos posOffset) {
      this.posOffset = posOffset;
   }

   @Override
   public boolean hasPosOffset() {
      return this.posOffset != BlockPos.field_177992_a;
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketMoveVehicle;x:D"
      )
   )
   private double preprocessPacketX(SPacketMoveVehicle _this) {
      return this.field_186960_a + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketMoveVehicle;y:D"
      )
   )
   private double preprocessPacketY(SPacketMoveVehicle _this) {
      return this.field_186961_b + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketMoveVehicle;z:D"
      )
   )
   private double preprocessPacketZ(SPacketMoveVehicle _this) {
      return this.field_186962_c + (double)this.posOffset.func_177952_p();
   }
}
