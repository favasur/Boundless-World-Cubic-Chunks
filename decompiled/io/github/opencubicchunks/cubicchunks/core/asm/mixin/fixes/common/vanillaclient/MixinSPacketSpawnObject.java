package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnObject.class})
public class MixinSPacketSpawnObject implements IPositionPacket {
   @Shadow
   private double field_149016_b;
   @Shadow
   private double field_149017_c;
   @Shadow
   private double field_149014_d;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnObject() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnObject;x:D"
      )
   )
   private double preprocessPacketX(SPacketSpawnObject _this) {
      return this.field_149016_b + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnObject;y:D"
      )
   )
   private double preprocessPacketY(SPacketSpawnObject _this) {
      return this.field_149017_c + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnObject;z:D"
      )
   )
   private double preprocessPacketZ(SPacketSpawnObject _this) {
      return this.field_149014_d + (double)this.posOffset.func_177952_p();
   }
}
