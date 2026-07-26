package io.github.opencubicchunks.cubicchunks.api.world;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Map;
import net.minecraft.util.math.ChunkPos;

public interface ICubicTicket {
   Map<ChunkPos, IntSet> getAllForcedChunkCubes();
}
