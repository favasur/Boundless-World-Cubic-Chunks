package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketEntity;
import net.minecraft.network.play.server.SPacketEntity.S17PacketEntityLookMove;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({S17PacketEntityLookMove.class})
public class MixinSPacketEntityLookMove extends SPacketEntity implements IPositionPacket {
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketEntityLookMove() {
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
         target = "Lnet/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove;posX:I"
      )
   )
   private int preprocessPacketX(S17PacketEntityLookMove buf) {
      return this.field_149072_b + this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove;posY:I"
      )
   )
   private int preprocessPacketY(S17PacketEntityLookMove buf) {
      return this.field_149073_c + this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove;posZ:I"
      )
   )
   private int preprocessPacketZ(S17PacketEntityLookMove buf) {
      return this.field_149070_d + this.posOffset.func_177952_p();
   }
}
