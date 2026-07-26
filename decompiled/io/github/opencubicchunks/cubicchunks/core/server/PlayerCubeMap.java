package io.github.opencubicchunks.cubicchunks.core.server;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.util.XZMap;
import io.github.opencubicchunks.cubicchunks.api.world.CubeWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.util.WatchersSortingList;
import io.github.opencubicchunks.cubicchunks.core.visibility.CubeSelector;
import io.github.opencubicchunks.cubicchunks.core.visibility.CuboidalCubeSelector;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PlayerCubeMap extends PlayerChunkMap implements LightingManager.IHeightChangeListener {
   private static final Predicate<EntityPlayerMP> NOT_SPECTATOR = player -> player != null && !player.func_175149_v();
   private static final Predicate<EntityPlayerMP> CAN_GENERATE_CHUNKS = player -> player != null
         && (!player.func_175149_v() || player.func_71121_q().func_82736_K().func_82766_b("spectatorsGenerateChunks"));
   private static final Comparator<CubeWatcher> CUBE_ORDER = (watcher1, watcher2) -> ComparisonChain.start()
         .compare(watcher1.getClosestPlayerDistance(), watcher2.getClosestPlayerDistance())
         .result();
   private static final Comparator<ColumnWatcher> COLUMN_ORDER = (watcher1, watcher2) -> ComparisonChain.start()
         .compare(watcher1.func_187270_g(), watcher2.func_187270_g())
         .result();
   private final CubeSelector cubeSelector = new CuboidalCubeSelector();
   private final TIntObjectMap<PlayerCubeMap.PlayerWrapper> players = new TIntObjectHashMap();
   final XYZMap<CubeWatcher> cubeWatchers = new XYZMap<>(0.7F, 15625);
   final XZMap<ColumnWatcher> columnWatchers = new XZMap<>(0.7F, 625);
   private final Set<CubeWatcher> cubeWatchersToUpdate = new HashSet<>();
   private final Set<ColumnWatcher> columnWatchersToUpdate = new HashSet<>();
   private final Map<EntityPlayerMP, WatchersSortingList<CubeWatcher>> cubesToAddPlayerTo = new IdentityHashMap<>();
   private final WatchersSortingList<CubeWatcher> cubesToSendToClients = new WatchersSortingList<>(CUBE_ORDER);
   private final WatchersSortingList<CubeWatcher> cubesToGenerate = new WatchersSortingList<>(CUBE_ORDER);
   private final WatchersSortingList<ColumnWatcher> columnsToSendToClients = new WatchersSortingList<>(COLUMN_ORDER);
   private final WatchersSortingList<ColumnWatcher> columnsToGenerate = new WatchersSortingList<>(COLUMN_ORDER);
   private int horizontalViewDistance;
   private int verticalViewDistance;
   private long previousWorldTime = 0L;
   private boolean toGenerateNeedSort = true;
   private boolean toSendToClientNeedSort = true;
   private final CubeProviderServer cubeCache;
   private final Multimap<EntityPlayerMP, Cube> cubesToSend = Multimaps.newSetMultimap(new HashMap(), HashSet::new);
   private Set<EntityPlayerMP> pendingPlayerAdd = new HashSet<>();
   private final PlayerCubeMap.TickableChunkContainer tickableChunksCubesToReturn = new PlayerCubeMap.TickableChunkContainer();
   private final ChunkGc chunkGc;
   final VanillaNetworkHandler vanillaNetworkHandler;

   public PlayerCubeMap(WorldServer worldServer) {
      super(worldServer);
      this.cubeCache = ((ICubicWorldInternal.Server)worldServer).getCubeCache();
      this.setPlayerViewDistance(
         worldServer.func_73046_m().func_184103_al().func_72395_o(), ((ICubicPlayerList)worldServer.func_73046_m().func_184103_al()).getVerticalViewDistance()
      );
      ((ICubicWorldInternal)worldServer).getLightingManager().registerHeightChangeListener(this);
      this.chunkGc = new ChunkGc(((ICubicWorldInternal.Server)worldServer).getCubeCache());
      this.vanillaNetworkHandler = ((ICubicWorldInternal.Server)worldServer).getVanillaNetworkHandler();
   }

   @Deprecated
   public Iterator<Chunk> func_187300_b() {
      final Iterator<Chunk> chunkIt = this.cubeCache.func_189548_a().iterator();
      return new AbstractIterator<Chunk>() {
         protected Chunk computeNext() {
            while (chunkIt.hasNext()) {
               IColumn column = (IColumn)chunkIt.next();
               if (column.shouldTick()) {
                  return (Chunk)column;
               }
            }

            return (Chunk)this.endOfData();
         }
      };
   }

   public PlayerCubeMap.TickableChunkContainer getTickableChunks() {
      PlayerCubeMap.TickableChunkContainer tickableChunksCubes = this.tickableChunksCubesToReturn;
      tickableChunksCubes.clear();
      this.addTickableColumns(tickableChunksCubes);
      this.addTickableCubes(tickableChunksCubes);
      this.addForcedColumns(tickableChunksCubes);
      this.addForcedCubes(tickableChunksCubes);
      return tickableChunksCubes;
   }

   private void addForcedColumns(PlayerCubeMap.TickableChunkContainer tickableChunksCubes) {
      for (IColumn columns : ((ICubicWorldInternal.Server)this.func_72688_a()).getForcedColumns()) {
         tickableChunksCubes.addColumn((Chunk)columns);
      }
   }

   private void addForcedCubes(PlayerCubeMap.TickableChunkContainer tickableChunksCubes) {
      tickableChunksCubes.forcedCubes = ((ICubicWorldInternal.Server)this.func_72688_a()).getForcedCubes();
   }

   private void addTickableCubes(PlayerCubeMap.TickableChunkContainer tickableChunksCubes) {
      for (CubeWatcher watcher : this.cubeWatchers) {
         ICube cube = watcher.getCube();
         if (cube != null && watcher.hasPlayerMatchingInRange(NOT_SPECTATOR, 128)) {
            tickableChunksCubes.addCube(cube);
         }
      }
   }

   private void addTickableColumns(PlayerCubeMap.TickableChunkContainer tickableChunksCubes) {
      for (ColumnWatcher watcher : this.columnWatchers) {
         Chunk chunk = watcher.func_187266_f();
         if (chunk != null && watcher.func_187271_a(128.0, NOT_SPECTATOR)) {
            tickableChunksCubes.addColumn(chunk);
         }
      }
   }

   public void func_72693_b() {
      this.func_72688_a().field_72984_F.func_76320_a("playerCubeMapTick");
      long currentTime = this.func_72688_a().func_82737_E();
      this.func_72688_a().field_72984_F.func_76320_a("addPendingPlayers");
      if (!this.pendingPlayerAdd.isEmpty()) {
         Set<EntityPlayerMP> players = this.pendingPlayerAdd;
         this.pendingPlayerAdd = new HashSet<>();

         for (EntityPlayerMP player : players) {
            this.func_72683_a(player);
         }
      }

      this.func_72688_a().field_72984_F.func_76318_c("tickEntries");
      if (currentTime - this.previousWorldTime > 8000L) {
         this.previousWorldTime = currentTime;

         for (CubeWatcher playerInstance : this.cubeWatchers) {
            playerInstance.update();
            playerInstance.updateInhabitedTime();
         }
      }

      if (!this.cubeWatchersToUpdate.isEmpty()) {
         this.cubeWatchersToUpdate.forEach(CubeWatcher::update);
         this.cubeWatchersToUpdate.clear();
      }

      if (!this.columnWatchersToUpdate.isEmpty()) {
         this.columnWatchersToUpdate.forEach(ColumnWatcher::func_187280_d);
         this.columnWatchersToUpdate.clear();
      }

      this.func_72688_a().field_72984_F.func_76318_c("sortToGenerate");
      if (this.toGenerateNeedSort && currentTime % 4L == 0L) {
         this.toGenerateNeedSort = false;
         this.cubesToGenerate.sort();
         this.columnsToGenerate.sort();
      }

      this.func_72688_a().field_72984_F.func_76318_c("sortToSend");
      if (this.toSendToClientNeedSort && currentTime % 4L == 2L) {
         this.toSendToClientNeedSort = false;
         this.cubesToSendToClients.sort();
         this.columnsToSendToClients.sort();
         this.cubesToAddPlayerTo.forEach((p, set) -> set.sort());
      }

      this.func_72688_a().field_72984_F.func_76318_c("generate");
      if (!this.columnsToGenerate.isEmpty()) {
         this.func_72688_a().field_72984_F.func_76320_a("columns");
         Iterator<ColumnWatcher> iter = this.columnsToGenerate.iterator();

         while (iter.hasNext()) {
            ColumnWatcher entry = iter.next();
            boolean success = entry.func_187266_f() != null;
            if (!success) {
               boolean canGenerate = entry.func_187269_a(CAN_GENERATE_CHUNKS);
               this.func_72688_a().field_72984_F.func_76320_a("generate");
               success = entry.func_187268_a(canGenerate);
               this.func_72688_a().field_72984_F.func_76319_b();
            }

            if (success) {
               iter.remove();
               if (entry.func_187272_b()) {
                  this.columnsToSendToClients.remove(entry);
               }
            }
         }

         this.func_72688_a().field_72984_F.func_76319_b();
      }

      if (!this.cubesToGenerate.isEmpty()) {
         this.func_72688_a().field_72984_F.func_76320_a("cubes");
         long stopTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos((long)CubicChunksConfig.maxCubeGenerationTimeMillis);
         int chunksToGenerate = CubicChunksConfig.maxGeneratedCubesPerTick;
         Iterator<CubeWatcher> iterator = this.cubesToGenerate.iterator();

         while (iterator.hasNext() && chunksToGenerate >= 0 && System.nanoTime() < stopTime) {
            CubeWatcher watcher = iterator.next();
            if (!watcher.isWaitingForColumn()) {
               boolean successx = !watcher.isWaitingForCube() && !watcher.isWaitingForLighting();
               boolean alreadyLoaded = successx;
               if (!successx) {
                  boolean canGenerate = watcher.hasPlayerMatching(CAN_GENERATE_CHUNKS);
                  this.func_72688_a().field_72984_F.func_76320_a("generate");
                  successx = watcher.providePlayerCube(canGenerate);
                  this.func_72688_a().field_72984_F.func_76319_b();
               }

               if (successx) {
                  CubeWatcher.SendToPlayersResult state = watcher.sendToPlayers();
                  if (state == CubeWatcher.SendToPlayersResult.WAITING
                     || state == CubeWatcher.SendToPlayersResult.CUBE_SENT
                     || state == CubeWatcher.SendToPlayersResult.ALREADY_DONE) {
                     iterator.remove();
                     this.cubesToSendToClients.remove(watcher);
                  }

                  if (!alreadyLoaded) {
                     chunksToGenerate--;
                  }
               }
            }
         }

         this.func_72688_a().field_72984_F.func_76319_b();
      }

      this.func_72688_a().field_72984_F.func_76318_c("send");
      if (!this.columnsToSendToClients.isEmpty()) {
         this.func_72688_a().field_72984_F.func_76320_a("columns");
         Iterator<ColumnWatcher> it = this.columnsToSendToClients.iterator();

         while (it.hasNext()) {
            ColumnWatcher playerInstance = it.next();
            if (playerInstance.func_187272_b()) {
               it.remove();
            } else if (!this.columnsToGenerate.contains(playerInstance)) {
               this.columnsToGenerate.appendToStart(playerInstance);
            }
         }

         this.columnsToSendToClients.removeIf(ColumnWatcher::func_187272_b);
         this.func_72688_a().field_72984_F.func_76319_b();
      }

      if (!this.cubesToSendToClients.isEmpty()) {
         this.func_72688_a().field_72984_F.func_76320_a("cubes");
         int toSend = CubicChunksConfig.cubesToSendPerTick;
         Iterator<CubeWatcher> it = this.cubesToSendToClients.iterator();

         while (it.hasNext() && toSend > 0) {
            CubeWatcher playerInstance = it.next();
            CubeWatcher.SendToPlayersResult statex = playerInstance.sendToPlayers();
            if (statex == CubeWatcher.SendToPlayersResult.ALREADY_DONE || statex == CubeWatcher.SendToPlayersResult.CUBE_SENT) {
               it.remove();
               toSend--;
            } else if (statex == CubeWatcher.SendToPlayersResult.WAITING_LIGHT && !this.cubesToGenerate.contains(playerInstance)) {
               this.cubesToGenerate.appendToStart(playerInstance);
            }
         }

         this.func_72688_a().field_72984_F.func_76319_b();
      }

      if (!this.cubesToAddPlayerTo.isEmpty()) {
         boolean changed = false;
         Iterator<EntityPlayerMP> iterator = this.cubesToAddPlayerTo.keySet().iterator();

         while (iterator.hasNext()) {
            EntityPlayerMP entityPlayerMP = iterator.next();
            WatchersSortingList<CubeWatcher> watchers = this.cubesToAddPlayerTo.get(entityPlayerMP);
            int toSend = CubicChunksConfig.cubesToSendPerTick;

            Iterator<CubeWatcher> iter;
            for (iter = watchers.iterator(); toSend > 0 && iter.hasNext(); iter.remove()) {
               CubeWatcher watcher = iter.next();
               watcher.addPlayer(entityPlayerMP);
               changed = true;
               CubeWatcher.SendToPlayersResult statex = watcher.sendToPlayers();
               if ((statex == CubeWatcher.SendToPlayersResult.WAITING_LIGHT || statex == CubeWatcher.SendToPlayersResult.WAITING)
                  && !this.cubesToGenerate.contains(watcher)) {
                  this.cubesToGenerate.appendToStart(watcher);
               }

               if (statex != CubeWatcher.SendToPlayersResult.ALREADY_DONE) {
                  toSend--;
               }
            }

            if (!iter.hasNext()) {
               iterator.remove();
            }
         }

         if (changed) {
            this.setNeedSort();
         }
      }

      this.func_72688_a().field_72984_F.func_76318_c("unload");
      if (this.players.isEmpty()) {
         WorldProvider worldprovider = this.func_72688_a().field_73011_w;
         if (!worldprovider.func_76567_e()) {
            this.func_72688_a().func_72863_F().func_73240_a();
         }
      }

      this.func_72688_a().field_72984_F.func_76318_c("sendCubes");
      if (!this.cubesToSend.isEmpty()) {
         for (EntityPlayerMP player : this.cubesToSend.keySet()) {
            Collection<Cube> cubes = this.cubesToSend.get(player);
            if (!this.players.containsKey(player.func_145782_y())) {
               CubicChunks.LOGGER
                  .info("Skipping sending " + cubes.size() + " chunks to player " + player.func_70005_c_() + " that is no longer in this world!");
            } else {
               if (this.vanillaNetworkHandler.hasCubicChunks(player)) {
                  PacketCubes packet = new PacketCubes(new ArrayList<>(cubes));
                  PacketDispatcher.sendTo(packet, player);
               } else {
                  this.vanillaNetworkHandler.sendCubeLoadPackets(cubes, player);
               }

               for (Cube cube : cubes) {
                  ((ICubicEntityTracker)this.func_72688_a().func_73039_n()).sendLeashedEntitiesInCube(player, cube);
                  CubeWatcher watcherx = this.getCubeWatcher(cube.getCoords());

                  assert watcherx != null;

                  MinecraftForge.EVENT_BUS.post(new CubeWatchEvent(cube, cube.getCoords(), watcherx, player));
               }
            }
         }

         this.cubesToSend.clear();
      }

      this.func_72688_a().field_72984_F.func_76319_b();
      this.func_72688_a().field_72984_F.func_76319_b();
   }

   public boolean func_152621_a(int cubeX, int cubeZ) {
      return this.columnWatchers.get(cubeX, cubeZ) != null;
   }

   public PlayerChunkMapEntry func_187301_b(int cubeX, int cubeZ) {
      return this.columnWatchers.get(cubeX, cubeZ);
   }

   private CubeWatcher getOrCreateCubeWatcher(@Nonnull CubePos cubePos) {
      CubeWatcher cubeWatcher = this.cubeWatchers.get(cubePos.getX(), cubePos.getY(), cubePos.getZ());
      if (cubeWatcher == null) {
         cubeWatcher = new CubeWatcher(this, cubePos);
         this.cubeWatchers.put(cubeWatcher);
         if (cubeWatcher.isWaitingForColumn() || cubeWatcher.isWaitingForCube() || cubeWatcher.isWaitingForLighting()) {
            this.cubesToGenerate.appendToEnd(cubeWatcher);
         }

         this.cubesToSendToClients.appendToEnd(cubeWatcher);
      }

      return cubeWatcher;
   }

   private ColumnWatcher getOrCreateColumnWatcher(ChunkPos chunkPos) {
      ColumnWatcher columnWatcher = this.columnWatchers.get(chunkPos.field_77276_a, chunkPos.field_77275_b);
      if (columnWatcher == null) {
         columnWatcher = new ColumnWatcher(this, chunkPos);
         this.columnWatchers.put(columnWatcher);
         if (columnWatcher.func_187266_f() == null) {
            this.columnsToGenerate.appendToEnd(columnWatcher);
         }

         if (!columnWatcher.func_187272_b()) {
            this.columnsToSendToClients.appendToEnd(columnWatcher);
         }
      }

      return columnWatcher;
   }

   public void func_180244_a(BlockPos pos) {
      CubeWatcher cubeWatcher = this.getCubeWatcher(CubePos.fromBlockCoords(pos));
      if (cubeWatcher != null) {
         int localX = Coords.blockToLocal(pos.func_177958_n());
         int localY = Coords.blockToLocal(pos.func_177956_o());
         int localZ = Coords.blockToLocal(pos.func_177952_p());
         cubeWatcher.blockChanged(localX, localY, localZ);
      }
   }

   @Override
   public void heightUpdated(int blockX, int blockZ) {
      ColumnWatcher columnWatcher = this.columnWatchers.get(Coords.blockToCube(blockX), Coords.blockToCube(blockZ));
      if (columnWatcher != null) {
         int localX = Coords.blockToLocal(blockX);
         int localZ = Coords.blockToLocal(blockZ);
         columnWatcher.heightChanged(localX, localZ);
      }
   }

   public void func_72683_a(EntityPlayerMP player) {
      if (player.field_70170_p != this.func_72688_a()) {
         CubicChunks.bigWarning(
            "Player world not the same ad PlayerCubeMap world! Adding anyway. This is very likely to cause issues! Player world dimension ID: %d, PlayerCubeMap dimension ID: %d",
            player.field_70170_p.field_73011_w.getDimension(),
            this.func_72688_a().field_73011_w.getDimension()
         );
      } else if (!player.field_70170_p.field_73010_i.contains(player)) {
         CubicChunks.LOGGER.debug("PlayerCubeMap (dimension {}): Adding player to pending to add list", this.func_72688_a().field_73011_w.getDimension());
         this.pendingPlayerAdd.add(player);
         return;
      }

      PlayerCubeMap.PlayerWrapper playerWrapper = new PlayerCubeMap.PlayerWrapper(player);
      playerWrapper.updateManagedPos();
      if (!this.vanillaNetworkHandler.hasCubicChunks(player)) {
         this.vanillaNetworkHandler.updatePlayerPosition(this, player, playerWrapper.getManagedCubePos());
      }

      CubePos playerCubePos = CubePos.fromEntity(player);
      this.cubeSelector.forAllVisibleFrom(playerCubePos, this.horizontalViewDistance, this.verticalViewDistance, currentPos -> {
         ColumnWatcher chunkWatcher = this.getOrCreateColumnWatcher(currentPos.chunkPos());
         if (!chunkWatcher.func_187275_d(player)) {
            chunkWatcher.func_187276_a(player);
         }

         CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(currentPos);
         this.scheduleAddPlayerToWatcher(cubeWatcher, player);
      });
      this.players.put(player.func_145782_y(), playerWrapper);
      this.setNeedSort();
   }

   public void func_72695_c(EntityPlayerMP player) {
      PlayerCubeMap.PlayerWrapper playerWrapper = (PlayerCubeMap.PlayerWrapper)this.players.get(player.func_145782_y());
      if (playerWrapper != null) {
         CubePos playerCubePos = CubePos.fromEntityCoords(player.field_71131_d, playerWrapper.managedPosY, player.field_71132_e);
         ObjectSet<ColumnWatcher> toSendUnload = new ObjectOpenHashSet((this.horizontalViewDistance * 2 + 1) * (this.horizontalViewDistance * 2 + 1) * 6);
         this.cubeSelector.forAllVisibleFrom(playerCubePos, this.horizontalViewDistance, this.verticalViewDistance, cubePos -> {
            CubeWatcher watcher = this.getCubeWatcher(cubePos);
            if (watcher != null) {
               this.removePlayerFromCubeWatcher(watcher, player);
            }

            ColumnWatcher columnWatcher = this.getColumnWatcher(cubePos.chunkPos());
            if (columnWatcher != null) {
               toSendUnload.add(columnWatcher);
            }
         });
         toSendUnload.stream().filter(watcher -> watcher.func_187275_d(player)).forEach(watcher -> watcher.func_187277_b(player));
         this.players.remove(player.func_145782_y());
         this.setNeedSort();
         this.vanillaNetworkHandler.removePlayer(player);
      }
   }

   public void func_72685_d(EntityPlayerMP player) {
      PlayerCubeMap.PlayerWrapper playerWrapper = (PlayerCubeMap.PlayerWrapper)this.players.get(player.func_145782_y());
      if (playerWrapper != null) {
         if (playerWrapper.cubePosChanged()) {
            this.updatePlayer(playerWrapper, playerWrapper.getManagedCubePos(), CubePos.fromEntity(player));
            playerWrapper.updateManagedPos();
            this.setNeedSort();
            if (!this.vanillaNetworkHandler.hasCubicChunks(player)) {
               this.vanillaNetworkHandler.updatePlayerPosition(this, player, playerWrapper.getManagedCubePos());
            }

            this.chunkGc.tick();
         }
      }
   }

   private void updatePlayer(PlayerCubeMap.PlayerWrapper entry, CubePos oldPos, CubePos newPos) {
      this.func_72688_a().field_72984_F.func_76320_a("updateMovedPlayer");
      Set<CubePos> cubesToRemove = new HashSet<>();
      Set<CubePos> cubesToLoad = new HashSet<>();
      Set<ChunkPos> columnsToRemove = new HashSet<>();
      Set<ChunkPos> columnsToLoad = new HashSet<>();
      this.func_72688_a().field_72984_F.func_76320_a("findChanges");
      this.cubeSelector
         .findChanged(oldPos, newPos, this.horizontalViewDistance, this.verticalViewDistance, cubesToRemove, cubesToLoad, columnsToRemove, columnsToLoad);
      this.func_72688_a().field_72984_F.func_76318_c("createColumns");
      columnsToLoad.forEach(pos -> {
         ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(pos);

         assert columnWatcher.func_187264_a().equals(pos);

         columnWatcher.func_187276_a(entry.playerEntity);
      });
      this.func_72688_a().field_72984_F.func_76318_c("createCubes");
      cubesToLoad.forEach(pos -> {
         CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(pos);

         assert cubeWatcher.getCubePos().equals(pos);

         this.scheduleAddPlayerToWatcher(cubeWatcher, entry.playerEntity);
      });
      this.func_72688_a().field_72984_F.func_76318_c("removeCubes");
      cubesToRemove.forEach(pos -> {
         CubeWatcher cubeWatcher = this.getCubeWatcher(pos);
         if (cubeWatcher != null) {
            assert cubeWatcher.getCubePos().equals(pos);

            this.removePlayerFromCubeWatcher(cubeWatcher, entry.playerEntity);
         }
      });
      this.func_72688_a().field_72984_F.func_76318_c("removeColumns");
      columnsToRemove.forEach(pos -> {
         ColumnWatcher columnWatcher = this.getColumnWatcher(pos);
         if (columnWatcher != null) {
            assert columnWatcher.func_187264_a().equals(pos);

            columnWatcher.func_187277_b(entry.playerEntity);
         }
      });
      this.func_72688_a().field_72984_F.func_76319_b();
      this.func_72688_a().field_72984_F.func_76319_b();
      this.setNeedSort();
   }

   private void removePlayerFromCubeWatcher(CubeWatcher cubeWatcher, EntityPlayerMP playerEntity) {
      if (!cubeWatcher.containsPlayer(playerEntity)) {
         WatchersSortingList<CubeWatcher> cubeWatchers = this.cubesToAddPlayerTo.get(playerEntity);
         if (cubeWatchers != null) {
            cubeWatchers.remove(cubeWatcher);
         }
      }

      cubeWatcher.removePlayer(playerEntity);
   }

   private void scheduleAddPlayerToWatcher(CubeWatcher cubeWatcher, EntityPlayerMP playerEntity) {
      this.cubesToAddPlayerTo.computeIfAbsent(playerEntity, p -> new WatchersSortingList<>(Comparator.comparingDouble(w -> {
            double dx = (double)w.getCubePos().getXCenter() - playerEntity.field_70165_t;
            double dy = (double)w.getCubePos().getYCenter() - playerEntity.field_70163_u;
            double dz = (double)w.getCubePos().getZCenter() - playerEntity.field_70161_v;
            return dx * dx + dy * dy + dz * dz;
         }))).appendToEnd(cubeWatcher);
   }

   public boolean func_72694_a(EntityPlayerMP player, int cubeX, int cubeZ) {
      ColumnWatcher columnWatcher = this.getColumnWatcher(new ChunkPos(cubeX, cubeZ));
      return columnWatcher != null && columnWatcher.func_187275_d(player) && columnWatcher.func_187274_e();
   }

   public boolean isPlayerWatchingCube(EntityPlayerMP player, int cubeX, int cubeY, int cubeZ) {
      CubeWatcher watcher = this.getCubeWatcher(new CubePos(cubeX, cubeY, cubeZ));
      return watcher != null && watcher.containsPlayer(player) && watcher.isSentToPlayers();
   }

   @Deprecated
   public final void func_152622_a(int newHorizontalViewDistance) {
      this.setPlayerViewDistance(newHorizontalViewDistance, this.verticalViewDistance);
   }

   public final void setPlayerViewDistance(int newHorizontalViewDistance, int newVerticalViewDistance) {
      if (this.players != null) {
         newHorizontalViewDistance = MathHelper.func_76125_a(newHorizontalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);
         newVerticalViewDistance = MathHelper.func_76125_a(newVerticalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);
         if (newHorizontalViewDistance != this.horizontalViewDistance || newVerticalViewDistance != this.verticalViewDistance) {
            int oldHorizontalViewDistance = this.horizontalViewDistance;
            int oldVerticalViewDistance = this.verticalViewDistance;
            if ((newHorizontalViewDistance >= oldHorizontalViewDistance || newVerticalViewDistance <= oldVerticalViewDistance)
               && (newHorizontalViewDistance <= oldHorizontalViewDistance || newVerticalViewDistance >= oldVerticalViewDistance)) {
               for (PlayerCubeMap.PlayerWrapper playerWrapper : this.players.valueCollection()) {
                  EntityPlayerMP player = playerWrapper.playerEntity;
                  CubePos playerPos = playerWrapper.getManagedCubePos();
                  if (newHorizontalViewDistance <= oldHorizontalViewDistance && newVerticalViewDistance <= oldVerticalViewDistance) {
                     Set<CubePos> cubesToUnload = new HashSet<>();
                     Set<ChunkPos> columnsToUnload = new HashSet<>();
                     this.cubeSelector
                        .findAllUnloadedOnViewDistanceDecrease(
                           playerPos,
                           oldHorizontalViewDistance,
                           newHorizontalViewDistance,
                           oldVerticalViewDistance,
                           newVerticalViewDistance,
                           cubesToUnload,
                           columnsToUnload
                        );
                     cubesToUnload.forEach(pos -> {
                        CubeWatcher cubeWatcher = this.getCubeWatcher(pos);
                        if (cubeWatcher != null) {
                           this.removePlayerFromCubeWatcher(cubeWatcher, player);
                        } else {
                           CubicChunks.LOGGER.warn("cubeWatcher null on render distance change");
                        }
                     });
                     columnsToUnload.forEach(pos -> {
                        ColumnWatcher columnWatcher = this.getColumnWatcher(pos);
                        if (columnWatcher != null && columnWatcher.func_187275_d(player)) {
                           columnWatcher.func_187277_b(player);
                        } else {
                           CubicChunks.LOGGER.warn("cubeWatcher null or doesn't contain player on render distance change");
                        }
                     });
                  } else {
                     this.cubeSelector.forAllVisibleFrom(playerPos, newHorizontalViewDistance, newVerticalViewDistance, pos -> {
                        ColumnWatcher columnWatcher = this.getOrCreateColumnWatcher(pos.chunkPos());
                        if (!columnWatcher.func_187275_d(player)) {
                           columnWatcher.func_187276_a(player);
                        }

                        CubeWatcher cubeWatcher = this.getOrCreateCubeWatcher(pos);
                        if (!cubeWatcher.containsPlayer(player)) {
                           this.scheduleAddPlayerToWatcher(cubeWatcher, player);
                        }
                     });
                  }
               }

               this.horizontalViewDistance = newHorizontalViewDistance;
               this.verticalViewDistance = newVerticalViewDistance;
               this.setNeedSort();
            } else {
               this.setPlayerViewDistance(newHorizontalViewDistance, oldVerticalViewDistance);
               this.setPlayerViewDistance(newHorizontalViewDistance, newVerticalViewDistance);
            }
         }
      }
   }

   private void setNeedSort() {
      this.toGenerateNeedSort = true;
      this.toSendToClientNeedSort = true;
   }

   public void func_187304_a(PlayerChunkMapEntry entry) {
      throw new UnsupportedOperationException();
   }

   public void func_187305_b(PlayerChunkMapEntry entry) {
      throw new UnsupportedOperationException();
   }

   void addToUpdateEntry(CubeWatcher cubeWatcher) {
      this.cubeWatchersToUpdate.add(cubeWatcher);
   }

   void addToUpdateEntry(ColumnWatcher columnWatcher) {
      this.columnWatchersToUpdate.add(columnWatcher);
   }

   void removeEntry(CubeWatcher cubeWatcher) {
      if (!this.cubesToAddPlayerTo.isEmpty()) {
         for (WatchersSortingList<CubeWatcher> value : this.cubesToAddPlayerTo.values()) {
            if (value.contains(cubeWatcher)) {
               return;
            }
         }
      }

      cubeWatcher.invalidate();
      CubePos cubePos = cubeWatcher.getCubePos();
      cubeWatcher.updateInhabitedTime();
      CubeWatcher removed = this.cubeWatchers.remove(cubePos.getX(), cubePos.getY(), cubePos.getZ());

      assert removed == cubeWatcher : "Removed unexpected cube watcher";

      this.cubeWatchersToUpdate.remove(cubeWatcher);
      this.cubesToGenerate.remove(cubeWatcher);
      this.cubesToSendToClients.remove(cubeWatcher);
      if (cubeWatcher.getCube() != null) {
         cubeWatcher.getCube().getTickets().remove(cubeWatcher);
      }

      if (!this.cubesToAddPlayerTo.isEmpty()) {
         Iterator<WatchersSortingList<CubeWatcher>> iterator = this.cubesToAddPlayerTo.values().iterator();

         while (iterator.hasNext()) {
            WatchersSortingList<CubeWatcher> valuex = iterator.next();
            valuex.remove(cubeWatcher);
            if (valuex.isEmpty()) {
               iterator.remove();
            }
         }
      }
   }

   public void removeEntry(ColumnWatcher entry) {
      ChunkPos pos = entry.func_187264_a();
      entry.func_187279_c();
      this.columnWatchers.remove(pos.field_77276_a, pos.field_77275_b);
      this.columnsToGenerate.remove(entry);
      this.columnsToSendToClients.remove(entry);
      this.columnWatchersToUpdate.remove(entry);
   }

   public void scheduleSendCubeToPlayer(Cube cube, EntityPlayerMP player) {
      this.cubesToSend.put(player, cube);
   }

   public void removeSchedulesSendCubeToPlayer(Cube cube, EntityPlayerMP player) {
      this.cubesToSend.remove(player, cube);
   }

   @Nullable
   public CubeWatcher getCubeWatcher(CubePos pos) {
      return this.cubeWatchers.get(pos.getX(), pos.getY(), pos.getZ());
   }

   @Nullable
   public ColumnWatcher getColumnWatcher(ChunkPos pos) {
      return this.columnWatchers.get(pos.field_77276_a, pos.field_77275_b);
   }

   public boolean contains(CubePos coords) {
      return this.cubeWatchers.get(coords.getX(), coords.getY(), coords.getZ()) != null;
   }

   public Iterator<CubeWatcher> getRandomWrappedCubeWatcherIterator(int seed) {
      return this.cubeWatchers.randomWrappedIterator(seed);
   }

   public Iterator<Cube> getCubeIterator() {
      WorldServer world = this.func_72688_a();
      final Iterator<CubeWatcher> iterator = this.cubeWatchers.iterator();
      ImmutableSetMultimap<ChunkPos, Ticket> persistentChunksFor = ForgeChunkManager.getPersistentChunksFor(world);
      world.field_72984_F.func_76320_a("forcedChunkLoading");
      final Iterator<Cube> persistentCubesIterator = ((ArrayList)persistentChunksFor.keys()
            .stream()
            .filter(Objects::nonNull)
            .map(input -> ((IColumn)world.func_72964_e(input.field_77276_a, input.field_77275_b)).getLoadedCubes())
            .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll))
         .iterator();
      world.field_72984_F.func_76319_b();
      return new AbstractIterator<Cube>() {
         Iterator<Cube> persistentCubes = persistentCubesIterator;

         boolean shouldSkip(Cube cube) {
            if (cube == null) {
               return true;
            } else {
               return cube.isEmpty() ? true : !cube.isFullyPopulated();
            }
         }

         protected Cube computeNext() {
            while (this.persistentCubes != null && this.persistentCubes.hasNext()) {
               Cube cube = this.persistentCubes.next();
               if (!this.persistentCubes.hasNext()) {
                  this.persistentCubes = null;
               }

               if (!this.shouldSkip(cube)) {
                  return cube;
               }
            }

            while (iterator.hasNext()) {
               CubeWatcher watcher = iterator.next();
               Cube cubex = watcher.getCube();
               if (!this.shouldSkip(cubex) && watcher.hasPlayerMatchingInRange(PlayerCubeMap.NOT_SPECTATOR, 128)) {
                  return cubex;
               }
            }

            return (Cube)this.endOfData();
         }
      };
   }

   private static final class PlayerWrapper {
      final EntityPlayerMP playerEntity;
      private double managedPosY;

      PlayerWrapper(EntityPlayerMP player) {
         this.playerEntity = player;
      }

      void updateManagedPos() {
         this.playerEntity.field_71131_d = this.playerEntity.field_70165_t;
         this.managedPosY = this.playerEntity.field_70163_u;
         this.playerEntity.field_71132_e = this.playerEntity.field_70161_v;
      }

      int getManagedCubePosX() {
         return Coords.blockToCube(this.playerEntity.field_71131_d);
      }

      int getManagedCubePosY() {
         return Coords.blockToCube(this.managedPosY);
      }

      int getManagedCubePosZ() {
         return Coords.blockToCube(this.playerEntity.field_71132_e);
      }

      CubePos getManagedCubePos() {
         return new CubePos(this.getManagedCubePosX(), this.getManagedCubePosY(), this.getManagedCubePosZ());
      }

      boolean cubePosChanged() {
         return Coords.blockToCube(this.playerEntity.field_70165_t) != this.getManagedCubePosX()
            || Coords.blockToCube(this.playerEntity.field_70163_u) != this.getManagedCubePosY()
            || Coords.blockToCube(this.playerEntity.field_70161_v) != this.getManagedCubePosZ();
      }
   }

   public class TickableChunkContainer {
      private final ObjectArrayList<ICube> cubes = ObjectArrayList.wrap(new ICube[65536]);
      private XYZMap<ICube> forcedCubes;
      private final Set<Chunk> columns = Collections.newSetFromMap(new IdentityHashMap<>());

      public TickableChunkContainer() {
      }

      private void clear() {
         this.cubes.clear();
         this.columns.clear();
      }

      private void addCube(ICube cube) {
         this.cubes.add(cube);
      }

      public void addColumn(Chunk column) {
         this.columns.add(column);
      }

      public Iterable<ICube> forcedCubes() {
         return this.forcedCubes;
      }

      public ICube[] playerTickableCubes() {
         return (ICube[])this.cubes.elements();
      }

      public Iterable<Chunk> columns() {
         return this.columns;
      }
   }
}
