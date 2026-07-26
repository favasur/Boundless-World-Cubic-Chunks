package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal
public interface ICubicTicketInternal extends ITicket {
    CubePos getLastForcedCubePos();
    void setLastForcedCubePos(CubePos pos);
}
