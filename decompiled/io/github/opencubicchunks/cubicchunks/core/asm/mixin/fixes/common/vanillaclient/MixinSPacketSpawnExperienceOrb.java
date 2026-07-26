package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnExperienceOrb;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnExperienceOrb.class})
public class MixinSPacketSpawnExperienceOrb implements IPositionPacket {
   @Shadow
   private double field_148990_b;
   @Shadow
   private double field_148991_c;
   @Shadow
   private double field_148988_d;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnExperienceOrb() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnExperienceOrb;posX:D"
      )
   )
   private double preprocessPacketX(SPacketSpawnExperienceOrb _this) {
      return this.field_148990_b + (double)this.posOffset.func_177958_n();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnExperienceOrb;posY:D"
      )
   )
   private double preprocessPacketY(SPacketSpawnExperienceOrb _this) {
      return this.field_148991_c + (double)this.posOffset.func_177956_o();
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketSpawnExperienceOrb;posZ:D"
      )
   )
   private double preprocessPacketZ(SPacketSpawnExperienceOrb _this) {
      return this.field_148988_d + (double)this.posOffset.func_177952_p();
   }
}
