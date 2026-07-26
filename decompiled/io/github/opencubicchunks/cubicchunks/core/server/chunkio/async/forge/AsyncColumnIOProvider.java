package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import java.io.IOException;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent.Load;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class AsyncColumnIOProvider extends AsyncIOProvider<Chunk> {
   @Nonnull
   private final ICubeIO loader;
   @Nullable
   private ICubeIO.PartialData<Chunk> columnData;
   @Nonnull
   private final QueuedColumn colInfo;
   private ICubeGenerator generator;
   private final Consumer<Chunk> setProviderLoadingColumn;

   AsyncColumnIOProvider(QueuedColumn colInfo, ICubeIO loader, ICubeGenerator generator, Consumer<Chunk> setProviderLoadingColumn) {
      this.loader = loader;
      this.colInfo = colInfo;
      this.generator = generator;
      this.setProviderLoadingColumn = setProviderLoadingColumn;
   }

   @Override
   public void run() {
      try {
         this.columnData = this.loader.loadColumnAsyncPart(this.colInfo.world, this.colInfo.x, this.colInfo.z);
      } catch (IOException var13) {
         CubicChunks.LOGGER.error("Could not load column in {} @ ({}, {})", this.colInfo.world, this.colInfo.x, this.colInfo.z, var13);
      } finally {
         synchronized (this) {
            this.finished = true;
            this.notifyAll();
         }
      }
   }

   @Override
   void runSynchronousPart() {
      assert this.columnData != null;

      if (this.columnData.getObject() != null) {
         this.loader.loadColumnSyncPart(this.columnData);
         Chunk column = this.columnData.getObject();

         assert column != null;

         try {
            this.setProviderLoadingColumn.accept(column);
            MinecraftForge.EVENT_BUS.post(new Load(column, this.columnData.getNbt()));
         } finally {
            this.setProviderLoadingColumn.accept(null);
         }

         column.func_177432_b(this.colInfo.world.func_82737_E());
         this.generator.recreateStructures(column);
      }

      this.runCallbacks();
   }

   @Nullable
   Chunk get() {
      return this.columnData == null ? null : this.columnData.getObject();
   }
}
