package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.LoadingData;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.BiConsumer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IThreadedFileIO;

public interface ICubeIO extends Flushable, AutoCloseable, IThreadedFileIO {
   @Override
   void flush() throws IOException;

   @Override
   void close() throws IOException;

   default ICubeIO.PartialData<Chunk> loadColumnAsyncPart(World world, int chunkX, int chunkZ) throws IOException {
      ICubeIO.PartialData<Chunk> data = this.loadColumnNbt(chunkX, chunkZ);
      Collection<BiConsumer<? super World, ? super LoadingData<ChunkPos>>> asyncCallbacks = CubeGeneratorsRegistry.getColumnAsyncLoadingCallbacks();
      if (!asyncCallbacks.isEmpty()) {
         ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
         LoadingData<ChunkPos> chunkLoadingData = new LoadingData<>(chunkPos, data.getNbt());
         asyncCallbacks.forEach(cons -> cons.accept(world, chunkLoadingData));
         data.setNbt(chunkLoadingData.getNbt());
      }

      this.loadColumnAsyncPart(data, chunkX, chunkZ);
      return data;
   }

   ICubeIO.PartialData<Chunk> loadColumnNbt(int var1, int var2) throws IOException;

   void loadColumnAsyncPart(ICubeIO.PartialData<Chunk> var1, int var2, int var3);

   void loadColumnSyncPart(ICubeIO.PartialData<Chunk> var1);

   default ICubeIO.PartialData<ICube> loadCubeAsyncPart(Chunk column, int cubeY) throws IOException {
      ICubeIO.PartialData<ICube> data = this.loadCubeNbt(column, cubeY);
      Collection<BiConsumer<? super World, ? super LoadingData<CubePos>>> asyncCallbacks = CubeGeneratorsRegistry.getCubeAsyncLoadingCallbacks();
      if (!asyncCallbacks.isEmpty()) {
         CubePos cubePos = new CubePos(column.field_76635_g, cubeY, column.field_76647_h);
         LoadingData<CubePos> chunkLoadingData = new LoadingData<>(cubePos, data.getNbt());
         asyncCallbacks.forEach(cons -> cons.accept(column.func_177412_p(), chunkLoadingData));
         data.setNbt(chunkLoadingData.getNbt());
      }

      this.loadCubeAsyncPart(data, column, cubeY);
      return data;
   }

   ICubeIO.PartialData<ICube> loadCubeNbt(Chunk var1, int var2) throws IOException;

   void loadCubeAsyncPart(ICubeIO.PartialData<ICube> var1, Chunk var2, int var3);

   void loadCubeSyncPart(ICubeIO.PartialData<ICube> var1);

   void saveColumn(Chunk var1);

   void saveCube(Cube var1);

   boolean cubeExists(int var1, int var2, int var3);

   boolean columnExists(int var1, int var2);

   int getPendingColumnCount();

   int getPendingCubeCount();

   public static class PartialData<T> {
      NBTTagCompound nbt;
      T object;

      public PartialData(T object, NBTTagCompound nbt) {
         this.object = object;
         this.nbt = nbt;
      }

      public T getObject() {
         return this.object;
      }

      public void setObject(T obj) {
         this.object = obj;
      }

      public NBTTagCompound getNbt() {
         return this.nbt;
      }

      public void setNbt(NBTTagCompound nbt) {
         this.nbt = nbt;
      }
   }
}
