package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnPainting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnPainting.class})
public class MixinSPacketSpawnPainting implements IPositionPacket {
   @Shadow
   private BlockPos field_179838_b;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnPainting() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnPainting;position:Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos preprocessPacket(SPacketSpawnPainting _this) {
      return this.posOffset == BlockPos.field_177992_a ? this.field_179838_b : this.field_179838_b.func_177971_a(this.posOffset);
   }
}
