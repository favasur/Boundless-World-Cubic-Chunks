package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketSpawnPosition.class})
public class MixinSPacketSpawnPosition implements IPositionPacket {
   @Shadow
   private BlockPos field_179801_a;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketSpawnPosition() {
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
         target = "Lnet/minecraft/network/play/server/SPacketSpawnPosition;spawnBlockPos:Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos preprocessPacket(SPacketSpawnPosition _this) {
      BlockPos pos = this.posOffset == BlockPos.field_177992_a ? this.field_179801_a : this.field_179801_a.func_177971_a(this.posOffset);
      int y = pos.func_177956_o();
      return this.hasPosOffset() && (y > 2047 || y < -2047)
         ? new BlockPos(pos.func_177958_n(), MathHelper.func_76125_a(y, -2047, 2047), pos.func_177952_p())
         : pos;
   }
}
