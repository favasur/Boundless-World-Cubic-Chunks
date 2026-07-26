package io.github.opencubicchunks.cubicchunks.core.network;

import com.google.common.base.Preconditions;
import gnu.trove.list.TByteList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketHeightMapUpdate implements IMessage {
   private ChunkPos chunk;
   private TByteList updates;
   private TIntList heights;

   public PacketHeightMapUpdate() {
   }

   public PacketHeightMapUpdate(ChunkPos chunk, TByteList updates, IHeightMap heightMap) {
      this.chunk = chunk;
      this.updates = new TByteArrayList();
      this.heights = new TIntArrayList();

      for (int i = 0; i < updates.size(); i++) {
         byte pos = updates.get(i);
         if (!this.updates.contains(pos)) {
            this.updates.add(pos);
            this.heights.add(heightMap.getTopBlockY(AddressTools.getLocalX(pos), AddressTools.getLocalZ(pos)));
         }
      }
   }

   public void fromBytes(ByteBuf buf) {
      this.chunk = new ChunkPos(buf.readInt(), buf.readInt());
      int size = buf.readUnsignedByte();
      this.updates = new TByteArrayList(size);
      this.heights = new TIntArrayList(size);

      for (int i = 0; i < size; i++) {
         this.updates.add(buf.readByte());
         this.heights.add(ByteBufUtils.readVarInt(buf, 5));
      }
   }

   public void toBytes(ByteBuf buf) {
      buf.writeInt(this.chunk.field_77276_a);
      buf.writeInt(this.chunk.field_77275_b);
      buf.writeByte(this.updates.size());

      for (int i = 0; i < this.updates.size(); i++) {
         buf.writeByte(this.updates.get(i) & 255);
         ByteBufUtils.writeVarInt(buf, this.heights.get(i), 5);
      }
   }

   ChunkPos getColumnPos() {
      return (ChunkPos)Preconditions.checkNotNull(this.chunk);
   }

   TByteList getUpdates() {
      return this.updates;
   }

   TIntList getHeights() {
      return this.heights;
   }

   public static class Handler extends AbstractClientMessageHandler<PacketHeightMapUpdate> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketHeightMapUpdate message, MessageContext ctx) {
         ICubicWorldInternal.Client worldClient = (ICubicWorldInternal.Client)world;
         CubeProviderClient cubeCache = worldClient.getCubeCache();
         int columnX = message.getColumnPos().field_77276_a;
         int columnZ = message.getColumnPos().field_77275_b;
         Chunk column = cubeCache.provideColumn(columnX, columnZ);
         if (column instanceof EmptyChunk) {
            CubicChunks.LOGGER.error("Ignored block update to blank column {}", message.getColumnPos());
         } else {
            ClientHeightMap index = (ClientHeightMap)((IColumn)column).getOpacityIndex();
            LightingManager lm = worldClient.getLightingManager();
            int size = message.getUpdates().size();

            for (int i = 0; i < size; i++) {
               int packed = message.getUpdates().get(i) & 255;
               int x = AddressTools.getLocalX(packed);
               int z = AddressTools.getLocalZ(packed);
               int height = message.getHeights().get(i);
               int oldHeight = index.getTopBlockY(x, z);
               index.setHeight(x, z, height);
            }
         }
      }
   }
}
