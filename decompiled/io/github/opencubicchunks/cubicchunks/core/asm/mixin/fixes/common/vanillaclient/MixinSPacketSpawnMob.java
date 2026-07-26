package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnMob;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnMob.class})
public class MixinSPacketSpawnMob implements IPositionPacket {
   @Shadow
   private double field_149041_c;
   @Shadow
   private double field_149038_d;
   @Shadow
   private double field_149039_e;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnMob() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;x:D"
      )
   )
   private double preprocessPacketX(SPacketSpawnMob _this) {
      return this.field_149041_c + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;y:D"
      )
   )
   private double preprocessPacketY(SPacketSpawnMob _this) {
      return this.field_149038_d + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnMob;z:D"
      )
   )
   private double preprocessPacketZ(SPacketSpawnMob _this) {
      return this.field_149039_e + (double)this.posOffset.func_177952_p();
   }
}
