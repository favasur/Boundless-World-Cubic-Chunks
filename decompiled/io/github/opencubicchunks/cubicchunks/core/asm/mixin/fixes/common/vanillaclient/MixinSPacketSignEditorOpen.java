package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSignEditorOpen;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSignEditorOpen.class})
public class MixinSPacketSignEditorOpen implements IPositionPacket {
   @Shadow
   private BlockPos field_179778_a;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSignEditorOpen() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSignEditorOpen;signPosition:Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos preprocessPacket(SPacketSignEditorOpen _this) {
      return this.posOffset == BlockPos.field_177992_a ? this.field_179778_a : this.field_179778_a.func_177971_a(this.posOffset);
   }
}
