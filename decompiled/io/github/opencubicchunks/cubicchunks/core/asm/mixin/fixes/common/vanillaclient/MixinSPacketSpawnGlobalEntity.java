package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnGlobalEntity.class})
public class MixinSPacketSpawnGlobalEntity implements IPositionPacket {
   @Shadow
   private double field_149057_b;
   @Shadow
   private double field_149058_c;
   @Shadow
   private double field_149055_d;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnGlobalEntity() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnGlobalEntity;x:D"
      )
   )
   private double preprocessPacketX(SPacketSpawnGlobalEntity _this) {
      return this.field_149057_b + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnGlobalEntity;y:D"
      )
   )
   private double preprocessPacketY(SPacketSpawnGlobalEntity _this) {
      return this.field_149058_c + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnGlobalEntity;z:D"
      )
   )
   private double preprocessPacketZ(SPacketSpawnGlobalEntity _this) {
      return this.field_149055_d + (double)this.posOffset.func_177952_p();
   }
}
