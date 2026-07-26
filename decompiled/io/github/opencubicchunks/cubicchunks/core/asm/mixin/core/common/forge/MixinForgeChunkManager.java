package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.forge;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.UnmodifiableIterator;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({ForgeChunkManager.class})
public abstract class MixinForgeChunkManager {
   @Shadow(
      remap = false
   )
   private static Map<World, Multimap<String, Ticket>> tickets;
   @Shadow(
      remap = false
   )
   private static Map<World, ImmutableSetMultimap<ChunkPos, Ticket>> forcedChunks;
   @Shadow(
      remap = false
   )
   private static Map<String, LoadingCallback> callbacks;
   @Shadow(
      remap = false
   )
   private static BiMap<UUID, Ticket> pendingEntities;
   @Shadow(
      remap = false
   )
   private static int dormantChunkCacheSize;
   @Shadow(
      remap = false
   )
   private static Map<Object, Object> dormantChunkCache;
   @Shadow(
      remap = false
   )
   private static SetMultimap<String, Ticket> playerTickets;

   public MixinForgeChunkManager() {
   }

   @Shadow(
      remap = false
   )
   public static int getMaxTicketLengthFor(String modId) {
      throw new Error("WTF!?");
   }

   @Overwrite(
      remap = false
   )
   static void loadWorld(World world) {
      ArrayListMultimap<String, Ticket> newTickets = ArrayListMultimap.create();
      MixinForgeChunkManager.tickets.put(world, newTickets);
      forcedChunks.put(world, ImmutableSetMultimap.of());
      if (world instanceof WorldServer) {
         if (dormantChunkCacheSize != 0) {
            dormantChunkCache.put(world, CacheBuilder.newBuilder().maximumSize((long)dormantChunkCacheSize).build());
         }

         WorldServer worldServer = (WorldServer)world;
         File chunkDir = worldServer.getChunkSaveLocation();
         File chunkLoaderData = new File(chunkDir, "forcedchunks.dat");
         if (chunkLoaderData.exists() && chunkLoaderData.isFile()) {
            ArrayListMultimap<String, Ticket> loadedTickets = ArrayListMultimap.create();
            Map<String, ListMultimap<String, Ticket>> playerLoadedTickets = Maps.newHashMap();

            NBTTagCompound forcedChunkData;
            try {
               forcedChunkData = CompressedStreamTools.func_74797_a(chunkLoaderData);
            } catch (IOException var19) {
               FMLLog.log.warn("Unable to read forced chunk data at {} - it will be ignored", chunkLoaderData.getAbsolutePath(), var19);
               return;
            }

            NBTTagList ticketList = forcedChunkData.func_150295_c("TicketList", 10);

            for (int i = 0; i < ticketList.func_74745_c(); i++) {
               NBTTagCompound ticketHolder = ticketList.func_150305_b(i);
               String modId = ticketHolder.func_74779_i("Owner");
               boolean isPlayer = "forge".equals(modId);
               if (!isPlayer && !Loader.isModLoaded(modId)) {
                  FMLLog.log
                     .warn("Found chunkloading data for mod {} which is currently not available or active - it will be removed from the world save", modId);
               } else if (!isPlayer && !callbacks.containsKey(modId)) {
                  FMLLog.log
                     .warn(
                        "The mod {} has registered persistent chunkloading data but doesn't seem to want to be called back with it - it will be removed from the world save",
                        modId
                     );
               } else {
                  NBTTagList tickets = ticketHolder.func_150295_c("Tickets", 10);

                  for (int j = 0; j < tickets.func_74745_c(); j++) {
                     NBTTagCompound ticket = tickets.func_150305_b(j);
                     modId = ticket.func_74764_b("ModId") ? ticket.func_74779_i("ModId") : modId;
                     Type type = Type.values()[ticket.func_74771_c("Type")];
                     Ticket tick = CubicChunkManager.makeTicket(modId, type, world);
                     CubicChunkManager.onDeserializeTicket(ticket, tick);
                     if (ticket.func_74764_b("ModData")) {
                        ((ICubicTicketInternal)tick).setModData(ticket.func_74775_l("ModData"));
                     }

                     if (ticket.func_74764_b("Player")) {
                        ((ICubicTicketInternal)tick).setPlayer(ticket.func_74779_i("Player"));
                        if (!playerLoadedTickets.containsKey(tick.getModId())) {
                           playerLoadedTickets.put(modId, ArrayListMultimap.create());
                        }

                        playerLoadedTickets.get(tick.getModId()).put(tick.getPlayerName(), tick);
                     } else {
                        loadedTickets.put(modId, tick);
                     }

                     if (type == Type.ENTITY) {
                        ((ICubicTicketInternal)tick).setEntityChunkX(ticket.func_74762_e("chunkX"));
                        ((ICubicTicketInternal)tick).setEntityChunkZ(ticket.func_74762_e("chunkZ"));
                        UUID uuid = new UUID(ticket.func_74763_f("PersistentIDMSB"), ticket.func_74763_f("PersistentIDLSB"));
                        pendingEntities.put(uuid, tick);
                     }
                  }
               }
            }

            UnmodifiableIterator var20 = ImmutableSet.copyOf(pendingEntities.values()).iterator();

            while (var20.hasNext()) {
               Ticket tickx = (Ticket)var20.next();
               if (tickx.getType() == Type.ENTITY && tickx.getEntity() == null) {
                  world.func_72964_e(((ICubicTicketInternal)tickx).getEntityChunkX(), ((ICubicTicketInternal)tickx).getEntityChunkZ());
                  CubicChunkManager.onLoadEntityTicketChunk(world, tickx);
               }
            }

            var20 = ImmutableSet.copyOf(pendingEntities.values()).iterator();

            while (var20.hasNext()) {
               Ticket tickx = (Ticket)var20.next();
               if (tickx.getType() == Type.ENTITY && tickx.getEntity() == null) {
                  FMLLog.log.warn("Failed to load persistent chunkloading entity {} from store.", pendingEntities.inverse().get(tickx));
                  loadedTickets.remove(tickx.getModId(), tickx);
               }
            }

            pendingEntities.clear();

            for (String modId : loadedTickets.keySet()) {
               LoadingCallback loadingCallback = callbacks.get(modId);
               if (loadingCallback != null) {
                  int maxTicketLength = getMaxTicketLengthFor(modId);
                  List<Ticket> tickets = loadedTickets.get(modId);
                  if (loadingCallback instanceof OrderedLoadingCallback) {
                     OrderedLoadingCallback orderedLoadingCallback = (OrderedLoadingCallback)loadingCallback;
                     tickets = orderedLoadingCallback.ticketsLoaded(ImmutableList.copyOf(tickets), world, maxTicketLength);
                  }

                  if (tickets.size() > maxTicketLength) {
                     FMLLog.log.warn("The mod {} has too many open chunkloading tickets {}. Excess will be dropped", modId, tickets.size());
                     tickets.subList(maxTicketLength, tickets.size()).clear();
                  }

                  MixinForgeChunkManager.tickets.get(world).putAll(modId, tickets);
                  loadingCallback.ticketsLoaded(ImmutableList.copyOf(tickets), world);
               }
            }

            for (String modIdx : playerLoadedTickets.keySet()) {
               LoadingCallback loadingCallback = callbacks.get(modIdx);
               if (loadingCallback != null) {
                  ListMultimap<String, Ticket> ticketsx = playerLoadedTickets.get(modIdx);
                  if (loadingCallback instanceof PlayerOrderedLoadingCallback) {
                     PlayerOrderedLoadingCallback orderedLoadingCallback = (PlayerOrderedLoadingCallback)loadingCallback;
                     ticketsx = orderedLoadingCallback.playerTicketsLoaded(ImmutableListMultimap.copyOf(ticketsx), world);
                     playerTickets.putAll(ticketsx);
                  }

                  MixinForgeChunkManager.tickets.get(world).putAll("forge", ticketsx.values());
                  loadingCallback.ticketsLoaded(ImmutableList.copyOf(ticketsx.values()), world);
               }
            }
         }
      }
   }

   @Inject(
      method = {"saveWorld"},
      at = {@At(
         value = "CONSTANT",
         args = {"stringValue=ChunkListDepth"}
      )},
      locals = LocalCapture.CAPTURE_FAILHARD,
      remap = false
   )
   private static void onSaveTicket(
      World world,
      CallbackInfo ci,
      WorldServer worldServer,
      File chunkDir,
      File chunkLoaderData,
      NBTTagCompound forcedChunkData,
      NBTTagList ticketList,
      Multimap<String, Ticket> ticketSet,
      Iterator<String> var7,
      String modId,
      NBTTagCompound ticketHolder,
      NBTTagList tickets,
      Iterator<Ticket> var11,
      Ticket tick,
      NBTTagCompound ticket
   ) {
      CubicChunkManager.onSerializeTicket(ticket, tick);
   }
}
