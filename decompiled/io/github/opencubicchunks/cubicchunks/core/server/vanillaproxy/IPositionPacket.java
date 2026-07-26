package io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy;

import net.minecraft.util.math.BlockPos;

public interface IPositionPacket {
   void setPosOffset(BlockPos var1);

   boolean hasPosOffset();
}
