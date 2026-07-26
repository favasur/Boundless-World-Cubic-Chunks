package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChunkGc {
   private final CubeProviderServer cubeCache;
   private int tick = 0;

   public ChunkGc(CubeProviderServer cubeCache) {
      this.cubeCache = cubeCache;
   }

   public void tick() {
      this.cubeCache.field_73251_h.field_72984_F.func_76320_a("chunkGc");
      this.tick++;
      if (this.tick > CubicChunksConfig.chunkGCInterval) {
         this.tick = 0;
         this.chunkGc();
      }

      if (CubicChunks.DEBUG_ENABLED) {
         this.verifyColumnConsistency();
      }

      this.cubeCache.field_73251_h.field_72984_F.func_76319_b();
   }

   private void verifyColumnConsistency() {
      Iterator<Cube> cubeIt = this.cubeCache.cubesIterator();

      while (cubeIt.hasNext()) {
         Cube cube = cubeIt.next();
         IColumn cubeCol = cube.getColumn();
         Chunk storedCol = this.cubeCache.getLoadedColumn(cube.getX(), cube.getZ());
         if (storedCol == null) {
            throw new RuntimeException("Cube with no stored column!");
         }

         if (storedCol != cubeCol) {
            throw new RuntimeException("CubeColumn and StoredColumn are different!");
         }
      }

      Iterator<Chunk> columnIt = this.cubeCache.columnsIterator();
      int totalCubes = 0;

      while (columnIt.hasNext()) {
         Chunk storedColx = columnIt.next();
         Collection<Cube> storedColumnCubes = ((IColumn)storedColx).getLoadedCubes();

         for (Cube c : storedColumnCubes) {
            if (this.cubeCache.getLoadedCube(c.getCoords()) != c) {
               throw new RuntimeException("Cube in column not the same as stored cube!");
            }
         }

         totalCubes += storedColumnCubes.size();
      }

      if (totalCubes != this.cubeCache.getLoadedCubeCount()) {
         throw new RuntimeException("Counted " + totalCubes + " cubes in columns, but there are total of " + this.cubeCache.getLoadedCubeCount() + " cubes!");
      }
   }

   public void chunkGc() {
      Iterator<Cube> cubeIt = this.cubeCache.cubesIterator();

      while (cubeIt.hasNext()) {
         if (this.cubeCache.tryUnloadCube(cubeIt.next())) {
            cubeIt.remove();
         }
      }

      Iterator<Chunk> columnIt = this.cubeCache.columnsIterator();

      while (columnIt.hasNext()) {
         if (this.cubeCache.tryUnloadColumn(columnIt.next())) {
            columnIt.remove();
         }
      }
   }
}
