package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IForgeChunkManager;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.ForgeChunkManager.UnforceChunkEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(
   modid = "cubicchunks"
)
public class CubicChunkManager {
   private static final MethodHandle ticketConstructor = ReflectionUtil.constructHandle(Ticket.class, String.class, Type.class, World.class);

   public CubicChunkManager() {
   }

   public static void forceChunk(Ticket ticket, CubePos chunk) {
      if (ticket != null && chunk != null) {
         if (ticket.getType() == Type.ENTITY && ticket.getEntity() == null) {
            throw new RuntimeException("Attempted to use an entity ticket to force a chunk, without an entity");
         } else if (ticket.isPlayerTicket()
            ? IForgeChunkManager.getPlayerTickets().containsValue(ticket)
            : IForgeChunkManager.getTickets().get(ticket.world).containsEntry(ticket.getModId(), ticket)) {
            ((ICubicTicketInternal)ticket).addRequestedCube(chunk);
            Cube cube = (Cube)((ICubicWorld)ticket.world).getCubeFromCubeCoords(chunk);
            cube.getTickets().add((ICubicTicketInternal)ticket);
            MinecraftForge.EVENT_BUS.post(new ForceCubeEvent(ticket, chunk));
            if (((ICubicTicketInternal)ticket).getMaxCubeDepth() > 0
               && ((ICubicTicketInternal)ticket).requestedCubes().size() > ((ICubicTicketInternal)ticket).getMaxCubeDepth()) {
               CubePos removed = ((ICubicTicketInternal)ticket).requestedCubes().iterator().next();
               unforceChunk(ticket, removed);
            }
         } else {
            FMLLog.log.fatal("The mod {} attempted to force load a chunk with an invalid ticket. This is not permitted.", ticket.getModId());
         }
      }
   }

   public static void reorderChunk(Ticket ticket, CubePos chunk) {
      if (ticket != null && chunk != null && ((ICubicTicketInternal)ticket).requestedCubes().contains(chunk)) {
         ((ICubicTicketInternal)ticket).removeRequestedCube(chunk);
         ((ICubicTicketInternal)ticket).addRequestedCube(chunk);
      }
   }

   public static void unforceChunk(Ticket ticket, CubePos chunk) {
      if (ticket != null && chunk != null) {
         ((ICubicTicketInternal)ticket).removeRequestedCube(chunk);
         MinecraftForge.EVENT_BUS.post(new UnforceCubeEvent(ticket, chunk));
         Cube cube = (Cube)((ICubicWorld)ticket.world).getCubeFromCubeCoords(chunk);
         cube.getTickets().remove((ICubicTicketInternal)ticket);
      }
   }

   private static ModContainer getContainer(Object mod) {
      return (ModContainer)Loader.instance().getModObjectList().inverse().get(mod);
   }

   public static Ticket makeTicket(String str, Type type, World world) {
      try {
         return ReflectionUtil.cast((Object)ticketConstructor.invoke((String)str, (Type)type, (World)world));
      } catch (Throwable var4) {
         throw new RuntimeException(var4);
      }
   }

   public static void onDeserializeTicket(NBTTagCompound ticketNBT, Ticket ticket) {
      NBTTagCompound cubicNBT = ticketNBT.func_74775_l("cubicchunks");
      if (cubicNBT != null) {
         int entityCubeY = cubicNBT.func_74762_e("entityCubeY");
         Map<ChunkPos, IntSet> coordsMap = new HashMap<>();

         for (NBTBase entryTagBase : cubicNBT.func_150295_c("chunkMap", 10)) {
            NBTTagCompound entry = (NBTTagCompound)entryTagBase;
            int x = entry.func_74762_e("x");
            int z = entry.func_74762_e("z");
            IntSet cubes = new IntArraySet(entry.func_74759_k("cubes"));
            coordsMap.put(new ChunkPos(x, z), cubes);
         }

         ((ICubicTicketInternal)ticket).setAllForcedChunkCubes(coordsMap);
      }
   }

   public static void onSerializeTicket(NBTTagCompound ticket, Ticket tick) {
      if (!((ICubicTicketInternal)tick).getAllForcedChunkCubes().isEmpty()) {
         NBTTagCompound cubicNBT = new NBTTagCompound();
         cubicNBT.func_74768_a("entityCubeY", ((ICubicTicketInternal)tick).getEntityChunkY());
         NBTTagList chunkMap = new NBTTagList();
         ((ICubicTicketInternal)tick).getAllForcedChunkCubes().forEach((pos, cubes) -> {
            NBTTagCompound entry = new NBTTagCompound();
            entry.func_74768_a("x", pos.field_77276_a);
            entry.func_74768_a("z", pos.field_77275_b);
            entry.func_74783_a("cubes", cubes.toIntArray());
            chunkMap.func_74742_a(entry);
         });
         cubicNBT.func_74782_a("chunkMap", chunkMap);
         ticket.func_74782_a("cubicchunks", cubicNBT);
      }
   }

   public static void onLoadEntityTicketChunk(World world, Ticket tick) {
      if (((ICubicWorld)world).isCubicWorld()) {
         ICubicTicketInternal ticket = (ICubicTicketInternal)tick;
         ((ICubicWorld)world).getCubeFromCubeCoords(ticket.getEntityChunkX(), ticket.getEntityChunkY(), ticket.getEntityChunkZ());
      }
   }

   @SubscribeEvent
   public static void onForgeChunkManagerForceChunk(ForceChunkEvent event) {
      Ticket ticket = event.getTicket();
      World worldInstance = ticket.world;
      if (((ICubicWorld)worldInstance).isCubicWorld() && worldInstance instanceof WorldServer) {
         addForcedCubesHeuristic(event, ticket, (WorldServer)worldInstance);
      }
   }

   private static void addForcedCubesHeuristic(ForceChunkEvent event, Ticket ticket, WorldServer worldInstance) {
      IntSet yCoords = ((ICubicTicketInternal)ticket).getAllForcedChunkCubes().get(event.getLocation());
      if (yCoords != null && !yCoords.isEmpty()) {
         yCoords.forEach(
            cubeYx -> ((ICubicWorldInternal)ticket.world)
                  .getCubeFromCubeCoords(event.getLocation().field_77276_a, cubeYx, event.getLocation().field_77275_b)
                  .getTickets()
                  .add((ITicket)ticket)
         );
      } else {
         WorldServer world = worldInstance;
         PlayerCubeMap cubeMap = (PlayerCubeMap)worldInstance.func_184164_w();
         PlayerChunkMapEntry columnWatcher = cubeMap.func_187301_b(event.getLocation().field_77276_a, event.getLocation().field_77275_b);
         if (columnWatcher == null) {
            ((ICubicTicketInternal)ticket).setForcedChunkCubes(event.getLocation(), new IntArraySet());
         } else {
            List<EntityPlayerMP> players = columnWatcher.getWatchingPlayers();
            int verticalViewDistance = CubicChunksConfig.verticalCubeLoadDistance;
            if (yCoords == null) {
               yCoords = new IntArraySet(players.size() * verticalViewDistance * 3);
            }

            for (EntityPlayerMP player : players) {
               for (int dy = -verticalViewDistance; dy <= verticalViewDistance; dy++) {
                  int cubeY = Coords.getCubeYForEntity(player) + dy;
                  Cube cube = (Cube)((ICubicWorld)world).getCubeFromCubeCoords(event.getLocation().field_77276_a, cubeY, event.getLocation().field_77275_b);
                  cube.getTickets().add((ITicket)ticket);
                  yCoords.add(cubeY);
               }
            }

            ((ICubicTicketInternal)ticket).setForcedChunkCubes(event.getLocation(), yCoords);
         }
      }
   }

   @SubscribeEvent
   public static void onForgeChunkManagerUnforceChunk(UnforceChunkEvent event) {
      Ticket ticket = event.getTicket();
      World world = ticket.world;
      if (((ICubicWorld)world).isCubicWorld()) {
         IntIterator var3 = ((ICubicTicketInternal)ticket).getAllForcedChunkCubes().get(event.getLocation()).iterator();

         while (var3.hasNext()) {
            int cubeY = (Integer)var3.next();
            Cube cube = (Cube)((ICubicWorld)world).getCubeFromCubeCoords(event.getLocation().field_77276_a, cubeY, event.getLocation().field_77275_b);
            cube.getTickets().remove((ITicket)ticket);
         }

         ((ICubicTicketInternal)ticket).clearForcedChunkCubes(event.getLocation());
      }
   }

   public static int getCubeDepthFor(String modId) {
      return CubicChunksConfig.modMaxCubesPerChunkloadingTicket.getOrDefault(modId, CubicChunksConfig.defaultMaxCubesPerChunkloadingTicket);
   }
}
