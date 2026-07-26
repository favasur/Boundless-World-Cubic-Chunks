package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SPacketUpdateTileEntity.class})
public class MixinSPacketUpdateTileEntity implements IPositionPacket {
   @Shadow
   private BlockPos field_179824_a;
   @Shadow
   private NBTTagCompound field_148860_e;
   private BlockPos posOffset = BlockPos.field_177992_a;

   public MixinSPacketUpdateTileEntity() {
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
         target = "Lnet/minecraft/network/play/server/SPacketUpdateTileEntity;blockPos:Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos preprocessPacket(SPacketUpdateTileEntity _this) {
      return this.posOffset == BlockPos.field_177992_a ? this.field_179824_a : this.field_179824_a.func_177971_a(this.posOffset);
   }

   @Redirect(
      method = {"writePacketData"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/network/play/server/SPacketUpdateTileEntity;nbt:Lnet/minecraft/nbt/NBTTagCompound;"
      )
   )
   private NBTTagCompound preprocessPacketNBT(SPacketUpdateTileEntity _this) {
      if (this.hasPosOffset()) {
         NBTTagCompound copy = this.field_148860_e.func_74737_b();
         if (copy.func_74764_b("x")) {
            copy.func_74768_a("x", copy.func_74762_e("x") + this.posOffset.func_177958_n());
         }

         if (copy.func_74764_b("y")) {
            copy.func_74768_a("y", copy.func_74762_e("y") + this.posOffset.func_177956_o());
         }

         if (copy.func_74764_b("z")) {
            copy.func_74768_a("z", copy.func_74762_e("z") + this.posOffset.func_177952_p());
         }

         return copy;
      } else {
         return this.field_148860_e;
      }
   }
}
