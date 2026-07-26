package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({PacketBuffer.class})
public abstract class MixinPacketBufferBlockPosWrite {
   public MixinPacketBufferBlockPosWrite() {
   }

   @Shadow
   public abstract long readLong();

   @Shadow
   public abstract ByteBuf writeLong(long var1);

   @Shadow
   public abstract int func_150792_a();

   @Shadow
   public abstract PacketBuffer func_150787_b(int var1);

   @Overwrite
   public BlockPos func_179259_c() {
      long data = this.readLong();
      BlockPos pos = BlockPos.func_177969_a(data);
      return pos.func_177956_o() == -2048 ? new BlockPos(pos.func_177958_n(), this.func_150792_a(), pos.func_177952_p()) : pos;
   }

   @Overwrite
   public PacketBuffer func_179255_a(BlockPos pos) {
      int y = pos.func_177956_o();
      if (y <= 2047 && y >= -2047) {
         this.writeLong(pos.func_177986_g());
      } else {
         this.writeLong(new BlockPos(pos.func_177958_n(), -2048, pos.func_177952_p()).func_177986_g());
         this.func_150787_b(y);
      }

      return (PacketBuffer)this;
   }
}
