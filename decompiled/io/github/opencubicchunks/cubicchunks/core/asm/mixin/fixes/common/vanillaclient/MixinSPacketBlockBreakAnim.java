package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketBlockBreakAnim.class})
public class MixinSPacketBlockBreakAnim implements IPositionPacket {
   @Shadow
   private BlockPos field_179822_b;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketBlockBreakAnim() {
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
         target = "Lnet/minecraft/network/play/server/SPacketBlockBreakAnim;position:Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos preprocessPacket(SPacketBlockBreakAnim _this) {
      return this.posOffset == BlockPos.field_177992_a ? this.field_179822_b : this.field_179822_b.func_177971_a(this.posOffset);
   }
}
