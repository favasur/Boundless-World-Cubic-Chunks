package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class AsyncBatchingCubeIO implements ICubeIO {
   protected final ReadWriteLock lock = new ReentrantReadWriteLock();
   protected final World world;
   protected final ICubicStorage storage;
   protected final Map<ChunkPos, NBTTagCompound> pendingColumns = new ConcurrentHashMap<>();
   protected final Map<CubePos, NBTTagCompound> pendingCubes = new ConcurrentHashMap<>();
   protected volatile boolean open = true;

   public AsyncBatchingCubeIO(World world, ICubicStorage storage) throws IOException {
      this.world = Objects.requireNonNull(world, "world");
      this.storage = Objects.requireNonNull(storage, "storage");
   }

   protected void ensureOpen() {
      Preconditions.checkState(this.open, "already closed?!?");
   }

   public ICubicStorage getStorage() {
      return this.storage;
   }

   @Override
   public boolean columnExists(int columnX, int columnZ) {
      ChunkPos pos = new ChunkPos(columnX, columnZ);
      this.lock.readLock().lock();

      boolean var5;
      try {
         this.ensureOpen();
         return this.pendingColumns.containsKey(pos) || this.storage.columnExists(pos);
      } catch (IOException var9) {
         CubicChunks.LOGGER.catching(var9);
         var5 = false;
      } finally {
         this.lock.readLock().unlock();
      }

      return var5;
   }

   @Override
   public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
      CubePos pos = new CubePos(cubeX, cubeY, cubeZ);
      this.lock.readLock().lock();

      boolean var6;
      try {
         this.ensureOpen();
         return this.pendingCubes.containsKey(pos) || this.storage.cubeExists(pos);
      } catch (IOException var10) {
         CubicChunks.LOGGER.catching(var10);
         var6 = false;
      } finally {
         this.lock.readLock().unlock();
      }

      return var6;
   }

   @Override
   public ICubeIO.PartialData<Chunk> loadColumnNbt(int chunkX, int chunkZ) throws IOException {
      ChunkPos pos = new ChunkPos(chunkX, chunkZ);
      this.lock.readLock().lock();

      ICubeIO.PartialData var5;
      try {
         this.ensureOpen();
         NBTTagCompound nbt = this.pendingColumns.get(pos);
         if (nbt == null) {
            nbt = this.storage.readColumn(pos);
         }

         if (nbt != null) {
            nbt = FMLCommonHandler.instance().getDataFixer().func_188257_a(FixTypes.CHUNK, nbt);
         }

         var5 = new ICubeIO.PartialData(null, nbt);
      } finally {
         this.lock.readLock().unlock();
      }

      return var5;
   }

   @Override
   public ICubeIO.PartialData<ICube> loadCubeNbt(Chunk column, int cubeY) throws IOException {
      CubePos pos = new CubePos(column.field_76635_g, cubeY, column.field_76647_h);
      this.lock.readLock().lock();

      ICubeIO.PartialData var5;
      try {
         this.ensureOpen();
         NBTTagCompound nbt = this.pendingCubes.get(pos);
         if (nbt == null) {
            nbt = this.storage.readCube(pos);
         }

         var5 = new ICubeIO.PartialData(null, nbt);
      } finally {
         this.lock.readLock().unlock();
      }

      return var5;
   }

   @Override
   public void saveColumn(Chunk column) {
      this.lock.readLock().lock();

      try {
         this.ensureOpen();
         this.pendingColumns.put(column.func_76632_l(), IONbtWriter.write(column));
         column.func_177427_f(false);
         ThreadedFileIOBase.func_178779_a().func_75735_a(this);
      } finally {
         this.lock.readLock().unlock();
      }
   }

   @Override
   public void saveCube(Cube cube) {
      this.lock.readLock().lock();

      try {
         this.ensureOpen();
         this.pendingCubes.put(cube.getCoords(), IONbtWriter.write(cube));
         cube.markSaved();
         ThreadedFileIOBase.func_178779_a().func_75735_a(this);
      } finally {
         this.lock.readLock().unlock();
      }
   }

   @Override
   public int getPendingColumnCount() {
      this.lock.readLock().lock();

      int var1;
      try {
         this.ensureOpen();
         var1 = this.pendingColumns.size();
      } finally {
         this.lock.readLock().unlock();
      }

      return var1;
   }

   @Override
   public int getPendingCubeCount() {
      this.lock.readLock().lock();

      int var1;
      try {
         this.ensureOpen();
         var1 = this.pendingCubes.size();
      } finally {
         this.lock.readLock().unlock();
      }

      return var1;
   }

   @Override
   public void flush() throws IOException {
      this.lock.writeLock().lock();

      try {
         this.ensureOpen();
         this.drainQueueBlocking();
         this.storage.flush();
      } catch (InterruptedException var5) {
         CubicChunks.LOGGER.catching(var5);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   @Override
   public void close() throws IOException {
      this.lock.writeLock().lock();

      try {
         this.ensureOpen();
         this.drainQueueBlocking();
         this.storage.close();
         this.open = false;
      } catch (InterruptedException var5) {
         CubicChunks.LOGGER.catching(var5);
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   protected void drainQueueBlocking() throws InterruptedException {
      do {
         ThreadedFileIOBase.func_178779_a().func_75735_a(this);
         ThreadedFileIOBase.func_178779_a().func_75734_a();
      } while (!this.pendingColumns.isEmpty() || !this.pendingCubes.isEmpty());
   }

   public boolean func_75814_c() {
      try {
         Map<ChunkPos, NBTTagCompound> columnsSnapshot = new Object2ObjectOpenHashMap(this.pendingColumns.size());
         this.pendingColumns.forEach(columnsSnapshot::put);
         Map<CubePos, NBTTagCompound> cubesSnapshot = new Object2ObjectOpenHashMap(this.pendingCubes.size());
         this.pendingCubes.forEach(cubesSnapshot::put);
         this.storage.writeBatch(new ICubicStorage.NBTBatch(Collections.unmodifiableMap(columnsSnapshot), Collections.unmodifiableMap(cubesSnapshot)));
         columnsSnapshot.forEach(this.pendingColumns::remove);
         cubesSnapshot.forEach(this.pendingCubes::remove);
      } catch (IOException var3) {
         var3.printStackTrace();
      }

      return !this.pendingColumns.isEmpty() || !this.pendingCubes.isEmpty();
   }

   @Override
   public void loadColumnAsyncPart(ICubeIO.PartialData<Chunk> info, int chunkX, int chunkZ) {
      if (info.getNbt() != null) {
         Chunk chunk = IONbtReader.readColumn(this.world, chunkX, chunkZ, info.getNbt());
         info.setObject(chunk);
      }
   }

   @Override
   public void loadColumnSyncPart(ICubeIO.PartialData<Chunk> info) {
   }

   @Override
   public void loadCubeAsyncPart(ICubeIO.PartialData<ICube> info, Chunk column, int cubeY) {
      if (info.getNbt() != null) {
         Cube cube = IONbtReader.readCubeAsyncPart(column, column.field_76635_g, cubeY, column.field_76647_h, info.getNbt());
         info.setObject(cube);
      }
   }

   @Override
   public void loadCubeSyncPart(ICubeIO.PartialData<ICube> info) {
      IONbtReader.readCubeSyncPart((Cube)info.object, this.world, info.nbt);
   }
}
