package io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@EventBusSubscriber
public class AsyncWorldIOExecutor {
   private static final int BASE_THREADS = 1;
   private static final int PLAYERS_PER_THREAD = 50;
   private static final Map<QueuedCube, AsyncCubeIOProvider> cubeTasks = new ConcurrentHashMap<>(20000, 0.8F, 1);
   private static final Map<QueuedColumn, AsyncColumnIOProvider> columnTasks = Maps.newConcurrentMap();
   private static final AtomicInteger threadCounter = new AtomicInteger();
   private static final ThreadPoolExecutor cubeThreadPool = new ThreadPoolExecutor(
      1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
         Thread thread = new Thread(r, "Cube I/O Thread #" + threadCounter.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      }
   );
   private static final ThreadPoolExecutor columnThreadPool = new ThreadPoolExecutor(
      1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
         Thread thread = new Thread(r, "Column I/O Thread #" + threadCounter.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      }
   );
   private static final Multimap<QueuedColumn, QueuedCube> loadingCubesColumnMap = Multimaps.newMultimap(new ConcurrentHashMap(), Sets::newConcurrentHashSet);

   public AsyncWorldIOExecutor() {
   }

   @Nullable
   public static Cube syncCubeLoad(World world, ICubeIO loader, CubeProviderServer cache, int cubeX, int cubeY, int cubeZ) {
      Chunk column = cache.getColumn(cubeX, cubeZ, ICubeProviderServer.Requirement.LIGHT);
      QueuedCube key = new QueuedCube(cubeX, cubeY, cubeZ, world);
      AsyncCubeIOProvider task = cubeTasks.remove(key);
      if (task != null) {
         task.setColumn(column);
         runTask(task);
      } else {
         task = new AsyncCubeIOProvider(key, loader);
         task.setColumn(column);
         task.run();
      }

      task.runSynchronousPart();
      return task.get();
   }

   @Nullable
   public static Chunk syncColumnLoad(World world, ICubeIO loader, int x, int z, Consumer<Chunk> setLoadingColumnCallback) {
      QueuedColumn key = new QueuedColumn(x, z, world);
      AsyncColumnIOProvider task = columnTasks.remove(key);
      if (task != null) {
         runTask(task);
      } else {
         task = new AsyncColumnIOProvider(key, loader, ((ICubicWorldInternal.Server)world).getCubeCache().getCubeGenerator(), setLoadingColumnCallback);
         task.run();
      }

      task.runSynchronousPart();
      return task.get();
   }

   private static void runTask(AsyncCubeIOProvider task) {
      runTask(cubeThreadPool, task);
   }

   private static void runTask(AsyncColumnIOProvider task) {
      runTask(columnThreadPool, task);
   }

   private static void runTask(ThreadPoolExecutor executor, AsyncIOProvider<?> task) {
      if (!executor.remove(task)) {
         synchronized (task) {
            while (!task.isFinished()) {
               try {
                  task.wait();
               } catch (InterruptedException var5) {
                  Thread.currentThread().interrupt();
                  throw new RuntimeException("Failed to wait for cube/column load", var5);
               }
            }
         }
      } else {
         task.run();
      }
   }

   public static void queueCubeLoad(World world, ICubeIO loader, CubeProviderServer cache, int x, int y, int z, Consumer<Cube> runnable) {
      QueuedCube key = new QueuedCube(x, y, z, world);
      QueuedColumn columnKey = new QueuedColumn(x, z, world);
      AsyncCubeIOProvider task = cubeTasks.get(key);
      loadingCubesColumnMap.put(columnKey, key);
      if (task == null) {
         task = new AsyncCubeIOProvider(key, loader);
         task.addCallback(runnable);
         task.addCallback(c -> loadingCubesColumnMap.remove(columnKey, key));
         cubeTasks.put(key, task);
         cubeThreadPool.execute(task);
      } else {
         task.addCallback(runnable);
      }

      Chunk loadedIColumn;
      if ((loadedIColumn = cache.getLoadedColumn(x, z)) == null) {
         cache.asyncGetColumn(x, z, ICubeProviderServer.Requirement.LIGHT, task::setColumn);
      } else {
         task.setColumn(loadedIColumn);
      }
   }

   public static void queueColumnLoad(World world, ICubeIO loader, int x, int z, Consumer<Chunk> runnable, Consumer<Chunk> setLoadingColumnCallback) {
      QueuedColumn key = new QueuedColumn(x, z, world);
      AsyncColumnIOProvider task = columnTasks.get(key);
      if (task == null) {
         task = new AsyncColumnIOProvider(key, loader, ((ICubicWorldInternal.Server)world).getCubeCache().getCubeGenerator(), setLoadingColumnCallback);
         task.addCallback(runnable);
         columnTasks.put(key, task);
         columnThreadPool.execute(task);
      } else {
         task.addCallback(runnable);
      }
   }

   public static void dropQueuedCubeLoad(World world, int x, int y, int z, Consumer<Cube> runnable) {
      QueuedCube key = new QueuedCube(x, y, z, world);
      AsyncCubeIOProvider task = cubeTasks.get(key);
      if (task == null) {
         CubicChunks.LOGGER.warn("Attempting to drop cube that wasn't queued in {} @ ({}, {}, {})", world, x, y, z);
      } else {
         task.removeCallback(runnable);
         if (!task.hasCallbacks()) {
            cubeTasks.remove(key);
            cubeThreadPool.remove(task);
         }
      }
   }

   public static void dropQueuedColumnLoad(World world, int x, int z, Consumer<Chunk> runnable) {
      QueuedColumn key = new QueuedColumn(x, z, world);
      AsyncColumnIOProvider task = columnTasks.get(key);
      if (task == null) {
         CubicChunks.LOGGER.warn("Attempting to drop column that wasn't queued in {} @ ({}, {})", world, x, z);
      } else {
         task.removeCallback(runnable);
         if (!task.hasCallbacks()) {
            columnTasks.remove(key);
            columnThreadPool.remove(task);
         }
      }
   }

   public static void tick() {
      Iterator<AsyncCubeIOProvider> cubeItr = cubeTasks.values().iterator();

      while (cubeItr.hasNext()) {
         AsyncCubeIOProvider task = cubeItr.next();
         if (task.isFinished()) {
            task.runSynchronousPart();
            cubeItr.remove();
         }
      }

      Iterator<AsyncColumnIOProvider> columnIter = columnTasks.values().iterator();

      while (columnIter.hasNext()) {
         AsyncColumnIOProvider task = columnIter.next();
         if (task.isFinished()) {
            task.runSynchronousPart();
            columnIter.remove();
         }
      }
   }

   private static void adjustPoolSize(int players) {
      cubeThreadPool.setCorePoolSize(Math.max(1, players / 50));
   }

   public static boolean canDropColumn(World world, int x, int z) {
      return !loadingCubesColumnMap.containsKey(new QueuedColumn(x, z, world));
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(@Nonnull PlayerLoggedInEvent evt) {
      MinecraftServer server = evt.player.func_184102_h();
      if (server != null) {
         adjustPoolSize(server.func_71233_x());
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(@Nonnull PlayerLoggedOutEvent evt) {
      MinecraftServer server = evt.player.func_184102_h();
      if (server != null) {
         adjustPoolSize(server.func_71233_x());
      }
   }

   @SubscribeEvent
   public static void onWorldTick(WorldTickEvent evt) {
      if (evt.phase == Phase.END) {
         tick();
      }
   }
}
