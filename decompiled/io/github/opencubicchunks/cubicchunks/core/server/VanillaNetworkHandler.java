package io.github.opencubicchunks.cubicchunks.core.server;

import gnu.trove.list.TShortList;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ISPacketChunkData;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ISPacketMultiBlockChange;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient.INetHandlerPlayServer;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.network.play.server.SPacketMultiBlockChange.BlockUpdateData;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

public class VanillaNetworkHandler {
   private static final Map<Class<?>, Field[]> packetFields = new IdentityHashMap<>();
   private static final Set<UUID> bedrockPlayers = new HashSet<>();
   private final WorldServer world;
   private Map<EntityPlayerMP, CubePos> playerOffsets = new IdentityHashMap<>();
   private Map<EntityPlayerMP, CubePos> playerOffsetsC2S = new IdentityHashMap<>();
   private Map<EntityPlayerMP, Integer> expectedTeleportId = new IdentityHashMap<>();

   public VanillaNetworkHandler(WorldServer world) {
      this.world = world;
   }

   public static void addBedrockPlayer(EntityPlayerMP player) {
      bedrockPlayers.add(player.func_110124_au());
   }

   public static void removeBedrockPlayer(EntityPlayerMP player) {
      bedrockPlayers.remove(player.func_110124_au());
   }

   public static Packet<?> copyPacket(Packet<?> packetIn) {
      if (!CubicChunksConfig.allowVanillaClients) {
         return packetIn;
      } else {
         try {
            Field[] fields = packetFields.computeIfAbsent(packetIn.getClass(), VanillaNetworkHandler::collectFields);
            Constructor<?> constructor = packetIn.getClass().getConstructor();
            Packet<?> newPacket = (Packet<?>)constructor.newInstance();

            for (Field field : fields) {
               Object v = field.get(packetIn);
               field.set(newPacket, v);
            }

            return newPacket;
         } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException var9) {
            throw new Error(var9);
         }
      }
   }

   private static Field[] collectFields(Class<?> aClass) {
      return collectFieldList(aClass).toArray(new Field[0]);
   }

   private static List<Field> collectFieldList(Class<?> aClass) {
      List<Field> fields = new ArrayList<>();

      do {
         Field[] f = aClass.getDeclaredFields();

         for (Field field : f) {
            if (!Modifier.isStatic(field.getModifiers())) {
               field.setAccessible(true);
               fields.add(field);
            }
         }

         aClass = aClass.getSuperclass();
      } while (aClass != Object.class);

      return fields;
   }

   private CubePos getPlayerOffsetS2C(EntityPlayerMP player) {
      return this.playerOffsets.getOrDefault(player, CubePos.ZERO);
   }

   private CubePos getPlayerOffsetC2S(EntityPlayerMP player) {
      return this.playerOffsetsC2S.getOrDefault(player, CubePos.ZERO);
   }

   public void sendCubeLoadPackets(Collection<? extends ICube> cubes, EntityPlayerMP player) {
      if (CubicChunksConfig.allowVanillaClients) {
         CubePos offset = this.getPlayerOffsetS2C(player);
         Map<ChunkPos, List<ICube>> columns = cubes.stream().collect(Collectors.groupingBy(c -> c.getCoords().chunkPos()));

         for (Entry<ChunkPos, List<ICube>> chunkPosListEntry : columns.entrySet()) {
            ChunkPos pos = chunkPosListEntry.getKey();
            List<ICube> column = chunkPosListEntry.getValue();
            SPacketChunkData chunkData = constructChunkData(pos, column, offset, this.world.field_73011_w.func_191066_m());
            player.field_71135_a.func_147359_a(chunkData);
         }
      }
   }

   private void sendFullCubeLoadPackets(Collection<? extends ICube> cubes, EntityPlayerMP player, CubePos offset, Set<ChunkPos> sentAsFullChunks) {
      if (CubicChunksConfig.allowVanillaClients) {
         Map<Chunk, List<ICube>> columns = cubes.stream().collect(Collectors.groupingBy(ICube::getColumn));

         for (Entry<Chunk, List<ICube>> chunkPosListEntry : columns.entrySet()) {
            Chunk chunk = chunkPosListEntry.getKey();
            List<ICube> column = chunkPosListEntry.getValue();
            ChunkPos pos = chunk.func_76632_l();
            SPacketChunkData chunkData = sentAsFullChunks.contains(pos)
               ? constructChunkData(pos, column, offset, this.world.field_73011_w.func_191066_m())
               : constructFullChunkData(chunk, column, offset, this.world.field_73011_w.func_191066_m());
            player.field_71135_a.func_147359_a(chunkData);
         }
      }
   }

   private Set<ChunkPos> sendFullCubeLoadPacketsWhereNecessary(
      Collection<? extends ICube> cubes, EntityPlayerMP player, CubePos offset, Set<ChunkPos> clientLoadedChunks
   ) {
      if (!CubicChunksConfig.allowVanillaClients) {
         return Collections.emptySet();
      } else {
         Map<Chunk, List<ICube>> columns = cubes.stream().collect(Collectors.groupingBy(ICube::getColumn));
         Set<ChunkPos> sentAsFullChunks = Collections.emptySet();

         for (Entry<Chunk, List<ICube>> chunkPosListEntry : columns.entrySet()) {
            Chunk chunk = chunkPosListEntry.getKey();
            ChunkPos offsetPos = new ChunkPos(chunk.field_76635_g + offset.getX(), chunk.field_76647_h + offset.getZ());
            List<ICube> column = chunkPosListEntry.getValue();
            if (clientLoadedChunks.contains(offsetPos)) {
               player.field_71135_a.func_147359_a(constructChunkData(chunk.func_76632_l(), column, offset, this.world.field_73011_w.func_191066_m()));
            } else {
               player.field_71135_a.func_147359_a(constructFullChunkData(chunk, column, offset, this.world.field_73011_w.func_191066_m()));
               (sentAsFullChunks.isEmpty() ? (sentAsFullChunks = new HashSet<>()) : sentAsFullChunks).add(offsetPos);
            }
         }

         return sentAsFullChunks;
      }
   }

   public void sendColumnLoadPacket(Chunk chunk, EntityPlayerMP player) {
      if (CubicChunksConfig.allowVanillaClients) {
         player.field_71135_a.func_147359_a(this.constructChunkData(chunk, this.getPlayerOffsetS2C(player)));
      }
   }

   public void sendColumnUnloadPacket(ChunkPos pos, EntityPlayerMP player) {
      if (CubicChunksConfig.allowVanillaClients) {
         CubePos offset = this.getPlayerOffsetS2C(player);
         player.field_71135_a.func_147359_a(new SPacketUnloadChunk(pos.field_77276_a + offset.getX(), pos.field_77275_b + offset.getZ()));
      }
   }

   public void sendBlockChanges(TShortList dirtyBlocks, Cube cube, EntityPlayerMP player) {
      if (CubicChunksConfig.allowVanillaClients) {
         CubePos offset = this.getPlayerOffsetS2C(player);
         int posX = cube.getX() + offset.getX();
         int posY = cube.getY() + offset.getY();
         int posZ = cube.getZ() + offset.getZ();
         if (posY >= 0 && posY < 16) {
            if (dirtyBlocks.size() == 1) {
               int localAddress = dirtyBlocks.get(0);
               int x = Coords.localToBlock(cube.getX(), AddressTools.getLocalX(localAddress));
               int y = Coords.localToBlock(cube.getY(), AddressTools.getLocalY(localAddress));
               int z = Coords.localToBlock(cube.getZ(), AddressTools.getLocalZ(localAddress));
               SPacketBlockChange packet = new SPacketBlockChange(this.world, new BlockPos(x, y, z));
               player.field_71135_a.func_147359_a(packet);
            } else {
               BlockUpdateData[] updates = new BlockUpdateData[dirtyBlocks.size()];
               SPacketMultiBlockChange packet = new SPacketMultiBlockChange();

               for (int i = 0; i < dirtyBlocks.size(); i++) {
                  int localAddress = dirtyBlocks.get(i);
                  int x = AddressTools.getLocalX(localAddress);
                  int localY = AddressTools.getLocalY(localAddress);
                  int y = localY + Coords.cubeToMinBlock(posY);
                  int z = AddressTools.getLocalZ(localAddress);
                  short vanillaPos = (short)(x << 12 | z << 8 | y);
                  packet.getClass();
                  updates[i] = new BlockUpdateData(
                     packet,
                     vanillaPos,
                     cube.getBlockState(Coords.localToBlock(cube.getX(), x), Coords.localToBlock(cube.getY(), localY), Coords.localToBlock(cube.getZ(), z))
                  );
               }

               ((ISPacketMultiBlockChange)packet).setChangedBlocks(updates);
               ((ISPacketMultiBlockChange)packet).setChunkPos(new ChunkPos(posX, posZ));
               player.field_71135_a.func_147359_a(packet);
            }
         }
      }
   }

   public void updatePlayerPosition(PlayerCubeMap cubeMap, EntityPlayerMP player, CubePos managedPos) {
      if (CubicChunksConfig.allowVanillaClients) {
         CubePos offset = this.playerOffsets.get(player);
         boolean isFirst = offset == null;
         if (isFirst) {
            offset = CubePos.ZERO;
            this.playerOffsets.put(player, CubePos.ZERO);
         }

         int posX = managedPos.getX() + offset.getX();
         int posY = managedPos.getY() + offset.getY();
         int posZ = managedPos.getZ() + offset.getZ();
         boolean shouldSliceTransition = posY < 2 || posY >= 14;
         boolean isHorizontalSlices = CubicChunksConfig.vanillaClients.horizontalSlices
            && (!CubicChunksConfig.vanillaClients.horizontalSlicesBedrockOnly || bedrockPlayers.contains(player.func_110124_au()));
         if (isHorizontalSlices) {
            int horizontalSliceSize = CubicChunksConfig.vanillaClients.horizontalSliceSize;
            int maxHorizontalOffset = Math.max(Math.abs(posX), Math.abs(posZ));
            shouldSliceTransition |= maxHorizontalOffset >= Coords.blockToCube(horizontalSliceSize);
         }

         if (shouldSliceTransition) {
            int newXOffset = isHorizontalSlices ? -managedPos.getX() : 0;
            int newYOffset = 8 - managedPos.getY();
            int newZOffset = isHorizontalSlices ? -managedPos.getZ() : 0;
            CubePos newOffset = new CubePos(newXOffset, newYOffset, newZOffset);
            this.playerOffsets.put(player, newOffset);
            if (isFirst) {
               this.playerOffsetsC2S.put(player, newOffset);
            } else {
               this.switchPlayerOffset(cubeMap, player, offset, newOffset);
            }
         }
      }
   }

   public boolean receiveOffsetUpdateConfirm(EntityPlayerMP player, int teleportId) {
      if (CubicChunksConfig.allowVanillaClients && this.expectedTeleportId.remove(player, teleportId)) {
         this.playerOffsetsC2S.put(player, this.playerOffsets.get(player));
         return true;
      } else {
         return false;
      }
   }

   public void removePlayer(EntityPlayerMP player) {
      if (CubicChunksConfig.allowVanillaClients) {
         this.playerOffsets.remove(player);
         this.playerOffsetsC2S.remove(player);
         this.expectedTeleportId.remove(player);
      }
   }

   private void switchPlayerOffset(PlayerCubeMap cubeMap, EntityPlayerMP player, CubePos oldOffset, CubePos newOffset) {
      if (CubicChunksConfig.allowVanillaClients) {
         Set<ChunkPos> clientLoadedChunks = new HashSet<>();

         for (ColumnWatcher columnWatcher : cubeMap.columnWatchers) {
            if (columnWatcher.func_187274_e() && columnWatcher.func_187275_d(player)) {
               clientLoadedChunks.add(new ChunkPos(columnWatcher.getX() + oldOffset.getX(), columnWatcher.getZ() + oldOffset.getZ()));
            }
         }

         Set<ChunkPos> clientLoadedNonEmptyChunks = new HashSet<>();
         List<ICube> firstSendCubes = new ArrayList<>();
         List<ICube> secondSendCubes = new ArrayList<>();
         List<ICube> lastSendCubes = new ArrayList<>();

         for (CubeWatcher cubeWatcher : cubeMap.cubeWatchers) {
            if (cubeWatcher.isSentToPlayers() && cubeWatcher.containsPlayer(player)) {
               int cy = Math.abs(player.field_70162_ai - cubeWatcher.getY());
               int cx = Math.abs(player.field_70176_ah - cubeWatcher.getX());
               int cz = Math.abs(player.field_70164_aj - cubeWatcher.getZ());
               if (cx <= 1 && cz <= 1) {
                  if (cy <= 1) {
                     firstSendCubes.add(cubeWatcher.getCube());
                  }

                  secondSendCubes.add(cubeWatcher.getCube());
               } else {
                  lastSendCubes.add(cubeWatcher.getCube());
               }

               clientLoadedNonEmptyChunks.add(new ChunkPos(cubeWatcher.getX() + oldOffset.getX(), cubeWatcher.getZ() + oldOffset.getZ()));
            }
         }

         Set<ChunkPos> sentAsFullChunks = this.sendFullCubeLoadPacketsWhereNecessary(firstSendCubes, player, newOffset, clientLoadedChunks);
         int teleportId = ((INetHandlerPlayServer)player.field_71135_a).getTeleportId();
         if (++teleportId == Integer.MAX_VALUE) {
            teleportId = 0;
         }

         ((INetHandlerPlayServer)player.field_71135_a).setTeleportId(teleportId);
         int dx = Coords.cubeToMinBlock(newOffset.getX() - oldOffset.getX());
         int dy = Coords.cubeToMinBlock(newOffset.getY() - oldOffset.getY());
         int dz = Coords.cubeToMinBlock(newOffset.getZ() - oldOffset.getZ());
         this.expectedTeleportId.put(player, teleportId);
         SPacketPlayerPosLook tpPacket = new SPacketPlayerPosLook(
            (double)dx, (double)dy + 0.01, (double)dz, 0.0F, 0.0F, EnumSet.allOf(EnumFlags.class), teleportId
         );
         player.field_71135_a.func_147359_a(tpPacket);
         clientLoadedChunks.removeAll(sentAsFullChunks);
         clientLoadedChunks.stream().map(pos -> new SPacketUnloadChunk(pos.field_77276_a, pos.field_77275_b)).forEach(player.field_71135_a::func_147359_a);
         this.sendFullCubeLoadPackets(secondSendCubes, player, newOffset, sentAsFullChunks);
         this.sendFullCubeLoadPackets(lastSendCubes, player, newOffset, sentAsFullChunks);
         clientLoadedChunks.removeAll(clientLoadedNonEmptyChunks);
         if (!clientLoadedChunks.isEmpty()) {
            clientLoadedChunks.stream()
               .map(pos -> cubeMap.columnWatchers.get(pos.field_77276_a - oldOffset.getX(), pos.field_77275_b - oldOffset.getZ()).func_187266_f())
               .forEach(
                  chunk -> player.field_71135_a
                        .func_147359_a(constructFullChunkData(chunk, Collections.emptyList(), newOffset, this.world.field_73011_w.func_191066_m()))
               );
         }

         this.world.func_73039_n().func_72787_a(player);
         this.world.func_73039_n().func_180245_a(player);
      }
   }

   private static SPacketChunkData constructChunkData(ChunkPos pos, Iterable<ICube> cubes, CubePos offset, boolean hasSkyLight) {
      ICube[] cubesToSend = new ICube[16];
      int mask = getCubesToSend(cubes, offset, cubesToSend);
      SPacketChunkData chunkData = new SPacketChunkData();
      ISPacketChunkData dataAccess = (ISPacketChunkData)chunkData;
      dataAccess.setChunkX(pos.field_77276_a + offset.getX());
      dataAccess.setChunkZ(pos.field_77275_b + offset.getZ());
      dataAccess.setFullChunk(false);
      byte[] dataBuffer = new byte[computeBufferSize(cubesToSend, hasSkyLight, mask)];
      PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(dataBuffer));
      buf.writerIndex(0);
      int availableSections = writeData(buf, cubesToSend, hasSkyLight, mask);
      dataAccess.setAvailableSections(availableSections);
      dataAccess.setBuffer(dataBuffer);
      List<NBTTagCompound> teList = collectTileEntityTags(offset, cubesToSend);
      dataAccess.setTileEntityTags(teList);
      return chunkData;
   }

   private SPacketChunkData constructChunkData(Chunk chunk, CubePos offset) {
      SPacketChunkData chunkData = new SPacketChunkData();
      ISPacketChunkData dataAccess = (ISPacketChunkData)chunkData;
      dataAccess.setChunkX(chunk.field_76635_g + offset.getX());
      dataAccess.setChunkZ(chunk.field_76647_h + offset.getZ());
      dataAccess.setAvailableSections(0);
      dataAccess.setFullChunk(true);
      byte[] dataBuffer = new byte[computeBufferSize(chunk)];
      PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(dataBuffer));
      buf.writerIndex(0);
      writeData(buf, chunk);
      dataAccess.setBuffer(dataBuffer);
      dataAccess.setTileEntityTags(new ArrayList<>());
      return chunkData;
   }

   private static SPacketChunkData constructFullChunkData(Chunk chunk, Iterable<ICube> cubes, CubePos offset, boolean hasSkyLight) {
      ICube[] cubesToSend = new ICube[16];
      int mask = getCubesToSend(cubes, offset, cubesToSend);
      SPacketChunkData chunkData = new SPacketChunkData();
      ISPacketChunkData dataAccess = (ISPacketChunkData)chunkData;
      dataAccess.setChunkX(chunk.field_76635_g + offset.getX());
      dataAccess.setChunkZ(chunk.field_76647_h + offset.getZ());
      dataAccess.setFullChunk(true);
      byte[] dataBuffer = new byte[computeBufferSize(chunk, cubesToSend, hasSkyLight, mask)];
      PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(dataBuffer));
      buf.writerIndex(0);
      int availableSections = writeData(chunk, buf, cubesToSend, hasSkyLight, mask);
      dataAccess.setAvailableSections(availableSections);
      dataAccess.setBuffer(dataBuffer);
      List<NBTTagCompound> teList = collectTileEntityTags(offset, cubesToSend);
      dataAccess.setTileEntityTags(teList);
      return chunkData;
   }

   private static int getCubesToSend(Iterable<ICube> cubes, CubePos offset, ICube[] cubesToSend) {
      int mask = 0;

      for (ICube cube : cubes) {
         int idx = cube.getY() + offset.getY();
         if (idx >= 0 && idx < 16 && !cube.isEmpty()) {
            cubesToSend[idx] = cube;
            mask |= 1 << idx;
         }
      }

      return mask;
   }

   private static List<NBTTagCompound> collectTileEntityTags(CubePos offset, ICube[] cubesToSend) {
      List<NBTTagCompound> teList = new ArrayList<>();

      for (ICube c : cubesToSend) {
         if (c != null) {
            for (TileEntity value : c.getTileEntityMap().values()) {
               NBTTagCompound updateTag = value.func_189517_E_();
               if (updateTag.func_74764_b("x")) {
                  updateTag.func_74768_a("x", updateTag.func_74762_e("x") + Coords.cubeToMinBlock(offset.getX()));
               }

               if (updateTag.func_74764_b("y")) {
                  updateTag.func_74768_a("y", updateTag.func_74762_e("y") + Coords.cubeToMinBlock(offset.getY()));
               }

               if (updateTag.func_74764_b("z")) {
                  updateTag.func_74768_a("z", updateTag.func_74762_e("z") + Coords.cubeToMinBlock(offset.getZ()));
               }

               teList.add(updateTag);
            }
         }
      }

      return teList;
   }

   private static int computeBufferSize(ICube[] cubesToSend, boolean hasSkyLight, int mask) {
      int total = 0;
      int cubeCount = cubesToSend.length;

      for (int j = 0; j < cubeCount; j++) {
         ExtendedBlockStorage storage = getStorage(cubesToSend, j);
         if (storage != Chunk.field_186036_a && (mask & 1 << j) != 0) {
            total += storage.func_186049_g().func_186018_a();
            total += storage.func_76661_k().func_177481_a().length;
            if (hasSkyLight) {
               total += storage.func_76671_l().func_177481_a().length;
            }
         }
      }

      return total;
   }

   private static ExtendedBlockStorage getStorage(ICube[] cubesToSend, int idx) {
      return cubesToSend[idx] == null ? null : cubesToSend[idx].getStorage();
   }

   private static int computeBufferSize(Chunk chunk) {
      return 256;
   }

   private static int computeBufferSize(Chunk chunk, ICube[] cubesToSend, boolean hasSkyLight, int mask) {
      int total = 0;
      int cubeCount = cubesToSend.length;

      for (int j = 0; j < cubeCount; j++) {
         ExtendedBlockStorage storage = getStorage(cubesToSend, j);
         if (storage != Chunk.field_186036_a && !storage.func_76663_a() && (mask & 1 << j) != 0) {
            total += storage.func_186049_g().func_186018_a();
            total += storage.func_76661_k().func_177481_a().length;
            if (hasSkyLight) {
               total += storage.func_76671_l().func_177481_a().length;
            }
         }
      }

      return total + chunk.func_76605_m().length;
   }

   private static int writeData(PacketBuffer buf, ICube[] cubes, boolean hasSkylight, int mask) {
      int sentSections = 0;
      int cubeCount = cubes.length;

      for (int j = 0; j < cubeCount; j++) {
         ExtendedBlockStorage storage = getStorage(cubes, j);
         if (storage != Chunk.field_186036_a && (mask & 1 << j) != 0) {
            sentSections |= 1 << j;
            storage.func_186049_g().func_186009_b(buf);
            buf.writeBytes(storage.func_76661_k().func_177481_a());
            if (hasSkylight) {
               buf.writeBytes(storage.func_76671_l().func_177481_a());
            }
         }
      }

      return sentSections;
   }

   private static void writeData(PacketBuffer buf, Chunk chunk) {
      buf.writeBytes(chunk.func_76605_m());
   }

   private static int writeData(Chunk chunk, PacketBuffer buf, ICube[] cubes, boolean hasSkylight, int mask) {
      int sentSections = 0;
      int cubeCount = cubes.length;

      for (int j = 0; j < cubeCount; j++) {
         ExtendedBlockStorage storage = getStorage(cubes, j);
         if (storage != Chunk.field_186036_a && !storage.func_76663_a() && (mask & 1 << j) != 0) {
            sentSections |= 1 << j;
            storage.func_186049_g().func_186009_b(buf);
            buf.writeBytes(storage.func_76661_k().func_177481_a());
            if (hasSkylight) {
               buf.writeBytes(storage.func_76671_l().func_177481_a());
            }
         }
      }

      buf.writeBytes(chunk.func_76605_m());
      return sentSections;
   }

   public boolean hasCubicChunks(EntityPlayerMP player) {
      if (!CubicChunksConfig.allowVanillaClients) {
         return true;
      } else {
         NetHandlerPlayServer connection = player.field_71135_a;
         if (connection == null) {
            return false;
         } else {
            NetworkManager netManager = connection.field_147371_a;
            if (netManager == null) {
               return false;
            } else {
               Channel channel = netManager.channel();
               if (!(Boolean)channel.attr(NetworkRegistry.FML_MARKER).get()) {
                  return false;
               } else {
                  Attribute<NetworkDispatcher> attr = channel.attr(NetworkDispatcher.FML_DISPATCHER);
                  NetworkDispatcher networkDispatcher = (NetworkDispatcher)attr.get();
                  if (networkDispatcher == null) {
                     return false;
                  } else {
                     Map<String, String> modList = networkDispatcher.getModList();
                     return modList.containsKey("cubicchunks");
                  }
               }
            }
         }
      }
   }

   public BlockPos modifyPositionC2S(BlockPos position, EntityPlayerMP player) {
      if (!CubicChunksConfig.allowVanillaClients) {
         return position;
      } else {
         BlockPos offset = this.getPlayerOffsetC2S(player).getMinBlockPos();
         return new BlockPos(
            position.func_177958_n() - offset.func_177958_n(),
            position.func_177956_o() - offset.func_177956_o(),
            position.func_177952_p() - offset.func_177952_p()
         );
      }
   }

   public BlockPos getC2SOffset(EntityPlayerMP player) {
      return !CubicChunksConfig.allowVanillaClients ? BlockPos.field_177992_a : this.getPlayerOffsetC2S(player).getMinBlockPos();
   }

   public BlockPos getS2COffset(EntityPlayerMP player) {
      return !CubicChunksConfig.allowVanillaClients ? BlockPos.field_177992_a : this.getPlayerOffsetS2C(player).getMinBlockPos();
   }
}
