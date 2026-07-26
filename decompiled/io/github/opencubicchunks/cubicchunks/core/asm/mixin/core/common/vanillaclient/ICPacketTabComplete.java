package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketTabComplete.class})
public interface ICPacketTabComplete {
   @Accessor
   void setTargetBlock(BlockPos var1);
}
