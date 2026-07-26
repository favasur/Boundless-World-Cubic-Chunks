package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnPlayer.class})
public class MixinSPacketSpawnPlayer implements IPositionPacket {
   @Shadow
   private double field_148956_c;
   @Shadow
   private double field_148953_d;
   @Shadow
   private double field_148954_e;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnPlayer() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnPlayer;x:D"
      )
   )
   private double preprocessPacketX(SPacketSpawnPlayer _this) {
      return this.field_148956_c + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnPlayer;y:D"
      )
   )
   private double preprocessPacketY(SPacketSpawnPlayer _this) {
      return this.field_148953_d + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnPlayer;z:D"
      )
   )
   private double preprocessPacketZ(SPacketSpawnPlayer _this) {
      return this.field_148954_e + (double)this.posOffset.func_177952_p();
   }
}
