package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketCustomSound.class})
public class MixinSPacketCustomSound implements IPositionPacket {
   @Shadow
   private int field_186934_c;
   @Shadow
   private int field_186935_d;
   @Shadow
   private int field_186936_e;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketCustomSound() {
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
         target = "Lnet/minecraft/network/play/server/SPacketCustomSound;x:I"
      )
   )
   private int preprocessPacketX(SPacketCustomSound _this) {
      return this.field_186934_c + this.posOffset.func_177952_p();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketCustomSound;y:I"
      )
   )
   private int preprocessPacketY(SPacketCustomSound _this) {
      return this.field_186935_d + this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketCustomSound;z:I"
      )
   )
   private int preprocessPacketZ(SPacketCustomSound _this) {
      return this.field_186936_e + this.posOffset.func_177958_n();
   }
}
