package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketParticles.class})
public class MixinSPacketParticles implements IPositionPacket {
   @Shadow
   private float field_149234_b;
   @Shadow
   private float field_149235_c;
   @Shadow
   private float field_149232_d;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketParticles() {
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
         target = "Lnet/minecraft/network/play/server/SPacketParticles;xCoord:F"
      )
   )
   private float preprocessPacketX(SPacketParticles _this) {
      return this.field_149234_b + (float)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketParticles;yCoord:F"
      )
   )
   private float preprocessPacketY(SPacketParticles _this) {
      return this.field_149235_c + (float)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketParticles;zCoord:F"
      )
   )
   private float preprocessPacketZ(SPacketParticles _this) {
      return this.field_149232_d + (float)this.posOffset.func_177952_p();
   }
}
