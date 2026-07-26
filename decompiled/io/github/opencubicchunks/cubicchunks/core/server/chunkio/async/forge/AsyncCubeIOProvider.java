package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class AsyncCubeIOProvider extends AsyncIOProvider<Cube> {
   @Nonnull
   private final QueuedCube cubeInfo;
   @Nonnull
   private final ICubeIO loader;
   @Nonnull
   private final CompletableFuture<Chunk> futureColumn = new CompletableFuture<>();
   @Nullable
   private ICubeIO.PartialData<ICube> cubeData;

   AsyncCubeIOProvider(QueuedCube cube, ICubeIO loader) {
      this.cubeInfo = cube;
      this.loader = loader;
   }

   @Override
   public void run() {
      try {
         Chunk column = this.futureColumn.get();
         if (column.func_76621_g()) {
            this.cubeData = new ICubeIO.PartialData<>(null, null);
         } else {
            this.cubeData = this.loader.loadCubeAsyncPart(column, this.cubeInfo.y);
         }
      } catch (IOException var15) {
         CubicChunks.LOGGER.error("Could not load cube in {} @ ({}, {}, {})", this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z, var15);
      } catch (InterruptedException var16) {
         throw new Error(var16);
      } catch (ExecutionException var17) {
         throw new RuntimeException(var17);
      } finally {
         synchronized (this) {
            this.finished = true;
            this.notifyAll();
         }
      }
   }

   @Override
   public void runSynchronousPart() {
      assert this.cubeData != null;

      if (this.cubeData.getObject() != null) {
         this.loader.loadCubeSyncPart(this.cubeData);
         ICube cube = this.cubeData.getObject();

         assert cube != null;

         MinecraftForge.EVENT_BUS.post(new CubeDataEvent.Load(cube, this.cubeData.getNbt()));
      }

      this.runCallbacks();
   }

   @Nullable
   public Cube get() {
      return this.cubeData == null ? null : (Cube)this.cubeData.getObject();
   }

   public void setColumn(@Nullable Chunk chunk) {
      if (!this.futureColumn.isDone()) {
         this.futureColumn.complete(chunk);
      }
   }
}
