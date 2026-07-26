package io.github.opencubicchunks.cubicchunks.core.server;

import com.google.common.base.Predicate;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeUnWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubeBlockChange;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadCube;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubeWatcher implements ITicket, ICubeWatcher {
   private final Consumer<Cube> consumer;
   private final CubeProviderServer cubeCache;
   private PlayerCubeMap playerCubeMap;
   @Nullable
   private Cube cube;
   private final ObjectArrayList<EntityPlayerMP> players = ObjectArrayList.wrap(new EntityPlayerMP[0]);
   private final TShortList dirtyBlocks = new TShortArrayList(64);
   private final CubePos cubePos;
   private long previousWorldTime = 0L;
   private boolean sentToPlayers = false;
   private boolean loading = true;
   private boolean invalid = false;
   private int lightGenerationAttempts = 0;

   CubeWatcher(PlayerCubeMap playerCubeMap, CubePos cubePos) {
      this.cubePos = cubePos;
      this.playerCubeMap = playerCubeMap;
      this.cubeCache = ((ICubicWorldInternal.Server)playerCubeMap.func_72688_a()).getCubeCache();
      this.consumer = c -> {
         if (!this.invalid) {
            this.cube = c;
            this.loading = false;
            if (this.cube != null) {
               this.cube.getTickets().add(this);
            }
         }
      };
      this.cubeCache.asyncGetCube(cubePos.getX(), cubePos.getY(), cubePos.getZ(), ICubeProviderServer.Requirement.LOAD, this.consumer);
   }

   void addPlayer(EntityPlayerMP player) {
      if (this.players.contains(player)) {
         CubicChunks.LOGGER.debug("Failed to add player. {} already is in cube at {}", player, this.cubePos);
      } else {
         if (this.players.isEmpty()) {
            this.previousWorldTime = this.getWorldTime();
         }

         this.players.add(player);
         if (this.sentToPlayers) {
            this.sendToPlayer(player);
            ((ICubicEntityTracker)this.playerCubeMap.func_72688_a().func_73039_n()).sendLeashedEntitiesInCube(player, this.getCube());
         }
      }
   }

   void removePlayer(EntityPlayerMP player) {
      if (!this.players.contains(player)) {
         if (this.players.isEmpty()) {
            this.playerCubeMap.removeEntry(this);
         }
      } else if (this.cube == null) {
         this.players.remove(player);
         if (this.players.isEmpty()) {
            this.playerCubeMap.removeEntry(this);
         }
      } else {
         if (this.sentToPlayers) {
            PacketDispatcher.sendTo(new PacketUnloadCube(this.cubePos), player);
            this.playerCubeMap.removeSchedulesSendCubeToPlayer(this.cube, player);
         }

         this.players.remove(player);
         MinecraftForge.EVENT_BUS.post(new CubeUnWatchEvent(this.cube, this.cubePos, this, player));
         if (this.players.isEmpty()) {
            this.playerCubeMap.removeEntry(this);
         }
      }
   }

   void invalidate() {
      if (this.loading) {
         AsyncWorldIOExecutor.dropQueuedCubeLoad(
            this.playerCubeMap.func_72688_a(), this.cubePos.getX(), this.cubePos.getY(), this.cubePos.getZ(), c -> this.cube = c
         );
      }

      this.invalid = true;
   }

   boolean providePlayerCube(boolean canGenerate) {
      if (this.loading) {
         return false;
      } else if (this.isWaitingForColumn()) {
         return false;
      } else if (this.cube == null || canGenerate && (this.isWaitingForCube() || this.isWaitingForLighting())) {
         int cubeX = this.cubePos.getX();
         int cubeY = this.cubePos.getY();
         int cubeZ = this.cubePos.getZ();
         this.playerCubeMap.func_72688_a().field_72984_F.func_76320_a("getCube");
         if (canGenerate) {
            this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.LIGHT);

            assert this.cube != null;

            if (this.cube instanceof BlankCube) {
               this.cube = null;
               return false;
            }

            if (!this.cube.isFullyPopulated()) {
               return false;
            }
         } else {
            this.cube = this.cubeCache.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.LOAD);
         }

         if (this.cube != null) {
            this.cube.getTickets().add(this);
         }

         this.playerCubeMap.func_72688_a().field_72984_F.func_76318_c("light");
         if (this.cube != null) {
            LightingManager.CubeLightUpdateInfo info = this.cube.getCubeLightUpdateInfo();
            if (info != null) {
               info.tick();
               if (info.hasUpdates()) {
                  this.lightGenerationAttempts++;
               } else {
                  this.lightGenerationAttempts = 0;
               }
            }
         }

         this.playerCubeMap.func_72688_a().field_72984_F.func_76319_b();
         return this.cube != null && !this.isWaitingForLighting();
      } else {
         return true;
      }
   }

   @Override
   public boolean isSentToPlayers() {
      return this.sentToPlayers;
   }

   boolean isWaitingForCube() {
      return this.cube == null || !this.cube.isFullyPopulated() || !this.cube.isInitialLightingDone() || !this.cube.isSurfaceTracked();
   }

   boolean isWaitingForLighting() {
      return this.cube == null || this.cube.hasLightUpdates() && this.lightGenerationAttempts < 3;
   }

   boolean isWaitingForColumn() {
      ColumnWatcher columnEntry = this.playerCubeMap.getColumnWatcher(this.cubePos.chunkPos());
      return columnEntry == null || !columnEntry.func_187274_e();
   }

   CubeWatcher.SendToPlayersResult sendToPlayers() {
      if (this.sentToPlayers) {
         return CubeWatcher.SendToPlayersResult.ALREADY_DONE;
      } else if (this.isWaitingForCube()) {
         return CubeWatcher.SendToPlayersResult.WAITING;
      } else if (this.isWaitingForLighting()) {
         return CubeWatcher.SendToPlayersResult.WAITING_LIGHT;
      } else if (this.isWaitingForColumn()) {
         return CubeWatcher.SendToPlayersResult.WAITING;
      } else if (!this.playerCubeMap.getColumnWatcher(this.cubePos.chunkPos()).func_187274_e()) {
         return CubeWatcher.SendToPlayersResult.WAITING;
      } else {
         this.dirtyBlocks.clear();
         this.sentToPlayers = true;
         ObjectListIterator var1 = this.players.iterator();

         while (var1.hasNext()) {
            EntityPlayerMP playerEntry = (EntityPlayerMP)var1.next();
            this.sendToPlayer(playerEntry);
         }

         return CubeWatcher.SendToPlayersResult.CUBE_SENT;
      }
   }

   private void sendToPlayer(EntityPlayerMP player) {
      if (this.sentToPlayers) {
         assert this.cube != null;

         this.playerCubeMap.scheduleSendCubeToPlayer(this.cube, player);
      }
   }

   void updateInhabitedTime() {
      long now = this.getWorldTime();
      if (this.cube == null) {
         this.previousWorldTime = now;
      } else {
         long inhabitedTime = this.cube.getColumn().func_177416_w();
         inhabitedTime += now - this.previousWorldTime;
         this.cube.getColumn().func_177415_c(inhabitedTime);
         this.previousWorldTime = now;
      }
   }

   void blockChanged(int localX, int localY, int localZ) {
      if (this.dirtyBlocks.isEmpty()) {
         this.playerCubeMap.addToUpdateEntry(this);
      }

      this.dirtyBlocks.add((short)AddressTools.getLocalAddress(localX, localY, localZ));
   }

   void update() {
      if (this.sentToPlayers) {
         assert this.cube != null;

         if (!this.dirtyBlocks.isEmpty()) {
            World world = this.cube.getWorld();
            if (this.dirtyBlocks.size() >= ForgeModContainer.clumpingThreshold) {
               this.players.forEach(entry -> this.playerCubeMap.scheduleSendCubeToPlayer(this.cube, entry));
            } else {
               PacketCubeBlockChange packet = null;
               ObjectListIterator var3 = this.players.iterator();

               while (var3.hasNext()) {
                  EntityPlayerMP player = (EntityPlayerMP)var3.next();
                  if (this.playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                     if (packet == null) {
                        packet = new PacketCubeBlockChange(this.cube, this.dirtyBlocks);
                     }

                     PacketDispatcher.sendTo(packet, player);
                  } else {
                     this.playerCubeMap.vanillaNetworkHandler.sendBlockChanges(this.dirtyBlocks, this.cube, player);
                  }
               }

               this.dirtyBlocks.forEach(localAddress -> {
                  BlockPos pos = this.cube.localAddressToBlockPos(localAddress);
                  IBlockState state = this.cube.getBlockState(pos);
                  if (state.func_177230_c().hasTileEntity(state)) {
                     this.sendBlockEntityToAllPlayers(world.func_175625_s(pos));
                  }

                  return true;
               });
            }

            this.dirtyBlocks.clear();
         }
      }
   }

   private void sendBlockEntityToAllPlayers(@Nullable TileEntity blockEntity) {
      if (blockEntity != null) {
         Packet<?> packet = blockEntity.func_189518_D_();
         if (packet != null) {
            this.sendPacketToAllPlayers(packet);
         }
      }
   }

   boolean containsPlayer(EntityPlayerMP player) {
      return this.players.contains(player);
   }

   boolean hasPlayerMatching(Predicate<EntityPlayerMP> predicate) {
      for (EntityPlayerMP e : (EntityPlayerMP[])this.players.elements()) {
         if (e == null) {
            break;
         }

         if (predicate.apply(e)) {
            return true;
         }
      }

      return false;
   }

   boolean hasPlayerMatchingInRange(Predicate<EntityPlayerMP> predicate, int range) {
      double d = (double)(range * range);
      double cx = (double)this.cubePos.getXCenter();
      double cy = (double)this.cubePos.getYCenter();
      double cz = (double)this.cubePos.getZCenter();

      for (EntityPlayerMP e : (EntityPlayerMP[])this.players.elements()) {
         if (e == null) {
            break;
         }

         if (predicate.apply(e)) {
            double dist = cx - e.field_70165_t;
            dist *= dist;
            if (!(dist > d)) {
               double dy = cy - e.field_70163_u;
               dist += dy * dy;
               if (!(dist > d)) {
                  double dz = cz - e.field_70161_v;
                  dist += dz * dz;
                  if (!(dist > d)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private double getDistanceSq(CubePos cubePos, Entity entity) {
      double blockX = (double)cubePos.getXCenter();
      double blockY = (double)cubePos.getYCenter();
      double blockZ = (double)cubePos.getZCenter();
      double dx = blockX - entity.field_70165_t;
      double dy = blockY - entity.field_70163_u;
      double dz = blockZ - entity.field_70161_v;
      return dx * dx + dy * dy + dz * dz;
   }

   @Nullable
   public Cube getCube() {
      return this.cube;
   }

   double getClosestPlayerDistance() {
      double min = Double.MAX_VALUE;

      for (EntityPlayerMP entry : (EntityPlayerMP[])this.players.elements()) {
         if (entry == null) {
            break;
         }

         double dist = this.getDistanceSq(this.cubePos, entry);
         if (dist < min) {
            min = dist;
         }
      }

      return min;
   }

   private long getWorldTime() {
      return this.playerCubeMap.func_72688_a().func_72820_D();
   }

   private void sendPacketToAllPlayers(Packet<?> packet) {
      ObjectListIterator var2 = this.players.iterator();

      while (var2.hasNext()) {
         EntityPlayerMP entry = (EntityPlayerMP)var2.next();
         entry.field_71135_a.func_147359_a(packet);
      }
   }

   @Override
   public void sendPacketToAllPlayers(IMessage packet) {
      ObjectListIterator var2 = this.players.iterator();

      while (var2.hasNext()) {
         EntityPlayerMP entry = (EntityPlayerMP)var2.next();
         PacketDispatcher.sendTo(packet, entry);
      }
   }

   CubePos getCubePos() {
      return this.cubePos;
   }

   @Override
   public int getX() {
      return this.cubePos.getX();
   }

   @Override
   public int getY() {
      return this.cubePos.getY();
   }

   @Override
   public int getZ() {
      return this.cubePos.getZ();
   }

   @Override
   public boolean shouldTick() {
      return true;
   }

   public static enum SendToPlayersResult {
      ALREADY_DONE,
      CUBE_SENT,
      WAITING,
      WAITING_LIGHT;

      private SendToPlayersResult() {
      }
   }
}
