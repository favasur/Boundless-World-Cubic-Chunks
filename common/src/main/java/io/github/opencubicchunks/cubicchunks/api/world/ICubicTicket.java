package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.world.ICubicTicket
// 1.21: cubic ticket authority is delegated to the cube provider. Mods acquired
// tickets through CubicChunks.requestForceLoadCubes; this stub preserves the API.
public interface ICubicTicket {

    ChunkPos getChunkPos();

    BlockPos getCubeCenter();

    boolean isValid();

    CubicChunks.TicketType getType();
}
