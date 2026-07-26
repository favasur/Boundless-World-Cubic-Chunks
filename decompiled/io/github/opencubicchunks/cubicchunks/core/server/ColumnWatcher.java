package io.github.opencubicchunks.cubicchunks.core.server;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IPlayerChunkMapEntry;
import io.github.opencubicchunks.cubicchunks.core.network.PacketColumn;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.network.PacketHeightMapUpdate;
import io.github.opencubicchunks.cubicchunks.core.network.PacketUnloadColumn;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.async.forge.AsyncWorldIOExecutor;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent.UnWatch;
import net.minecraftforge.event.world.ChunkWatchEvent.Watch;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ColumnWatcher extends PlayerChunkMapEntry implements XZAddressable {
   @Nonnull
   private final PlayerCubeMap playerCubeMap;
   @Nonnull
   private final TByteList dirtyColumns = new TByteArrayList(64);

   ColumnWatcher(PlayerCubeMap playerCubeMap, ChunkPos pos) {
      super(playerCubeMap, pos.field_77276_a, pos.field_77275_b);
      this.playerCubeMap = playerCubeMap;
   }

   private IPlayerChunkMapEntry self() {
      return (IPlayerChunkMapEntry)this;
   }

   public boolean func_187268_a(boolean canGenerate) {
      if (this.self().isLoading()) {
         return false;
      } else if (this.self().getChunk() != null) {
         return true;
      } else {
         if (canGenerate) {
            Chunk chunk = this.playerCubeMap
               .func_72688_a()
               .func_72863_F()
               .func_186025_d(this.self().getPos().field_77276_a, this.self().getPos().field_77275_b);
            if (chunk.func_76621_g()) {
               return false;
            }

            this.self().setChunk(chunk);
         } else {
            this.self()
               .setChunk(this.playerCubeMap.func_72688_a().func_72863_F().func_186028_c(this.self().getPos().field_77276_a, this.self().getPos().field_77275_b));
         }

         return this.self().getChunk() != null;
      }
   }

   public void func_187276_a(EntityPlayerMP player) {
      assert this.func_187266_f() == null || this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

      if (this.self().getPlayerList().contains(player)) {
         CubicChunks.LOGGER
            .debug("Failed to expand player. {} already is in chunk {}, {}", player, this.func_187264_a().field_77276_a, this.func_187264_a().field_77275_b);
      } else {
         if (this.self().getPlayerList().isEmpty()) {
            this.self().setLastUpdateInhabitedTime(this.playerCubeMap.func_72688_a().func_82737_E());
         }

         this.self().getPlayerList().add(player);
         if (this.func_187274_e()) {
            if (this.playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
               PacketColumn message = new PacketColumn(this.func_187266_f());
               PacketDispatcher.sendTo(message, player);
            } else {
               this.playerCubeMap.vanillaNetworkHandler.sendColumnLoadPacket(this.func_187266_f(), player);
            }

            MinecraftForge.EVENT_BUS.post(new Watch(this.func_187266_f(), player));
         }
      }
   }

   public void func_187277_b(EntityPlayerMP player) {
      assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

      if (this.self().getPlayerList().contains(player)) {
         if (this.func_187266_f() == null) {
            this.self().getPlayerList().remove(player);
            if (this.self().getPlayerList().isEmpty()) {
               if (this.self().isLoading()) {
                  AsyncWorldIOExecutor.dropQueuedColumnLoad(
                     this.playerCubeMap.func_72688_a(),
                     this.func_187264_a().field_77276_a,
                     this.func_187264_a().field_77275_b,
                     c -> this.self().getLoadedRunnable().run()
                  );
               }

               this.playerCubeMap.removeEntry(this);
            }
         } else {
            if (this.func_187274_e()) {
               if (this.playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                  PacketDispatcher.sendTo(new PacketUnloadColumn(this.func_187264_a()), player);
               } else {
                  this.playerCubeMap.vanillaNetworkHandler.sendColumnUnloadPacket(this.func_187264_a(), player);
               }
            }

            this.self().getPlayerList().remove(player);
            MinecraftForge.EVENT_BUS.post(new UnWatch(this.func_187266_f(), player));
            if (this.self().getPlayerList().isEmpty()) {
               this.playerCubeMap.removeEntry(this);
            }
         }
      }
   }

   public boolean func_187272_b() {
      if (this.func_187274_e()) {
         return true;
      } else if (this.func_187266_f() == null) {
         return false;
      } else {
         assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

         try {
            PacketColumn message = new PacketColumn(this.func_187266_f());

            for (EntityPlayerMP player : this.self().getPlayerList()) {
               if (this.playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                  PacketDispatcher.sendTo(message, player);
               } else {
                  this.playerCubeMap.vanillaNetworkHandler.sendColumnLoadPacket(this.func_187266_f(), player);
               }
            }

            this.self().setSentToPlayers(true);
            return true;
         } catch (Throwable var4) {
            throw new RuntimeException(var4);
         }
      }
   }

   @Deprecated
   public void func_187278_c(EntityPlayerMP player) {
      assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());
   }

   @Deprecated
   public void func_187265_a(int x, int y, int z) {
      assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

      CubeWatcher watcher = this.playerCubeMap.getCubeWatcher(CubePos.fromBlockCoords(x, y, z));
      if (watcher != null) {
         watcher.blockChanged(x, y, z);
      }
   }

   public void func_187280_d() {
      if (this.func_187274_e()) {
         assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ()) : "Column watcher "
            + this
            + " at "
            + this.func_187264_a()
            + " contains column "
            + this.func_187266_f()
            + " but loaded column is "
            + this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

         if (!this.dirtyColumns.isEmpty()) {
            assert this.func_187266_f() != null;

            for (EntityPlayerMP player : this.self().getPlayerList()) {
               if (this.playerCubeMap.vanillaNetworkHandler.hasCubicChunks(player)) {
                  PacketDispatcher.sendTo(
                     new PacketHeightMapUpdate(this.func_187264_a(), this.dirtyColumns, ((IColumn)this.func_187266_f()).getOpacityIndex()), player
                  );
               }
            }

            this.dirtyColumns.clear();
         }
      }
   }

   @Override
   public int getX() {
      return this.func_187264_a().field_77276_a;
   }

   @Override
   public int getZ() {
      return this.func_187264_a().field_77275_b;
   }

   void heightChanged(int localX, int localZ) {
      if (this.func_187274_e()) {
         assert this.func_187266_f() == this.playerCubeMap.func_72688_a().func_72863_F().func_186026_b(this.getX(), this.getZ());

         if (this.dirtyColumns.isEmpty()) {
            this.playerCubeMap.addToUpdateEntry(this);
         }

         this.dirtyColumns.add((byte)AddressTools.getLocalAddress(localX, localZ));
      }
   }

   @Deprecated
   private List<EntityPlayerMP> getPlayers() {
      return this.self().getPlayerList();
   }
}
