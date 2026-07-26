package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketPlayerDigging.class})
public interface ICPacketPlayerDigging {
   @Accessor
   void setPosition(BlockPos var1);
}
