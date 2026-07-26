package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.fml.common.eventhandler.Event;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UnforceCubeEvent extends Event {
   public UnforceCubeEvent(Ticket ticket, CubePos pos) {
   }
}
