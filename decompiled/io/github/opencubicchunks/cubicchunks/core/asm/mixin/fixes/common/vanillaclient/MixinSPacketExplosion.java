package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketExplosion.class})
public class MixinSPacketExplosion implements IPositionPacket {
   @Shadow
   private double field_149158_a;
   @Shadow
   private double field_149156_b;
   @Shadow
   private double field_149157_c;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketExplosion() {
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
         target = "Lnet/minecraft/network/play/server/SPacketExplosion;posX:D"
      )
   )
   private double preprocessPacketX(SPacketExplosion _this) {
      return this.field_149158_a + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketExplosion;posY:D"
      )
   )
   private double preprocessPacketY(SPacketExplosion _this) {
      return this.field_149156_b + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketExplosion;posZ:D"
      )
   )
   private double preprocessPacketZ(SPacketExplosion _this) {
      return this.field_149157_c + (double)this.posOffset.func_177952_p();
   }
}
