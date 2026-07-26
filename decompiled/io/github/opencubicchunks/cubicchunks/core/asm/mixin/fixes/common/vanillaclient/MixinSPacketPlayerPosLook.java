package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import java.util.Set;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketPlayerPosLook.class})
public class MixinSPacketPlayerPosLook implements IPositionPacket {
   @Shadow
   private double field_148940_a;
   @Shadow
   private double field_148938_b;
   @Shadow
   private double field_148939_c;
   @Shadow
   private Set<EnumFlags> field_179835_f;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketPlayerPosLook() {
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
         target = "Lnet/minecraft/network/play/server/SPacketPlayerPosLook;x:D"
      )
   )
   private double preprocessPacketX(SPacketPlayerPosLook _this) {
      return this.field_179835_f.contains(EnumFlags.X) ? this.field_148940_a : this.field_148940_a + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketPlayerPosLook;y:D"
      )
   )
   private double preprocessPacketY(SPacketPlayerPosLook _this) {
      return this.field_179835_f.contains(EnumFlags.Y) ? this.field_148938_b : this.field_148938_b + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketPlayerPosLook;z:D"
      )
   )
   private double preprocessPacketZ(SPacketPlayerPosLook _this) {
      return this.field_179835_f.contains(EnumFlags.Z) ? this.field_148939_c : this.field_148939_c + (double)this.posOffset.func_177952_p();
   }
}
