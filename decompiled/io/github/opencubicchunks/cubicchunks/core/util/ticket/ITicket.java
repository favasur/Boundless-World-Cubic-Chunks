package io.github.opencubicchunks.cubicchunks.core.util.ticket;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ITicket {
   boolean shouldTick();
}
