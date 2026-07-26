package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSoundEffect.class})
public class MixinSPacketSoundEffect implements IPositionPacket {
   @Shadow
   private int field_149217_b;
   @Shadow
   private int field_149218_c;
   @Shadow
   private int field_149215_d;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSoundEffect() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSoundEffect;posX:I"
      )
   )
   private int preprocessPacketX(SPacketSoundEffect _this) {
      return this.field_149217_b + this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSoundEffect;posY:I"
      )
   )
   private int preprocessPacketY(SPacketSoundEffect _this) {
      return this.field_149218_c + this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSoundEffect;posZ:I"
      )
   )
   private int preprocessPacketZ(SPacketSoundEffect _this) {
      return this.field_149215_d + this.posOffset.func_177952_p();
   }
}
