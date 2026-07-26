package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketMultiBlockChange.BlockUpdateData;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SPacketMultiBlockChange.class})
public interface ISPacketMultiBlockChange {
   @Accessor
   void setChunkPos(ChunkPos var1);

   @Accessor
   void setChangedBlocks(BlockUpdateData[] var1);
}
