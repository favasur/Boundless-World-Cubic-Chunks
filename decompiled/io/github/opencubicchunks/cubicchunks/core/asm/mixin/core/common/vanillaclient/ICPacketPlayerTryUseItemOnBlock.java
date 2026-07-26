package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketPlayerTryUseItemOnBlock.class})
public interface ICPacketPlayerTryUseItemOnBlock {
   @Accessor
   void setPosition(BlockPos var1);
}
