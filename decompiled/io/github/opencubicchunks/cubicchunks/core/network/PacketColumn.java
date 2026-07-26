package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketColumn implements IMessage {
   private ChunkPos chunkPos;
   private byte[] data;

   public PacketColumn() {
   }

   public PacketColumn(Chunk column) {
      this.chunkPos = column.func_76632_l();
      this.data = new byte[WorldEncoder.getEncodedSize(column)];
      PacketBuffer out = new PacketBuffer(WorldEncoder.createByteBufForWrite(this.data));
      WorldEncoder.encodeColumn(out, column);
   }

   public void fromBytes(ByteBuf buf) {
      this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
      this.data = new byte[buf.readInt()];
      buf.readBytes(this.data);
   }

   public void toBytes(ByteBuf buf) {
      buf.writeInt(this.chunkPos.field_77276_a);
      buf.writeInt(this.chunkPos.field_77275_b);
      buf.writeInt(this.data.length);
      buf.writeBytes(this.data);
   }

   ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   byte[] getData() {
      return this.data;
   }

   public static class Handler extends AbstractClientMessageHandler<PacketColumn> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketColumn packet, MessageContext ctx) {
         ICubicWorld worldClient = (ICubicWorld)world;
         CubeProviderClient cubeCache = (CubeProviderClient)worldClient.getCubeCache();
         ChunkPos chunkPos = packet.getChunkPos();
         Chunk column = cubeCache.func_73158_c(chunkPos.field_77276_a, chunkPos.field_77275_b);
         byte[] data = packet.getData();
         ByteBuf buf = WorldEncoder.createByteBufForRead(data);
         WorldEncoder.decodeColumn(new PacketBuffer(buf), column);
      }
   }
}
