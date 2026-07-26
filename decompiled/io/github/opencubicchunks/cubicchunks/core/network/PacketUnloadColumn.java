package io.github.opencubicchunks.cubicchunks.core.network;

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketUnloadColumn implements IMessage {
   private ChunkPos chunkPos;

   public PacketUnloadColumn() {
   }

   public PacketUnloadColumn(ChunkPos chunkPos) {
      this.chunkPos = chunkPos;
   }

   public void fromBytes(ByteBuf buf) {
      this.chunkPos = new ChunkPos(buf.readInt(), buf.readInt());
   }

   public void toBytes(ByteBuf buf) {
      buf.writeInt(this.chunkPos.field_77276_a);
      buf.writeInt(this.chunkPos.field_77275_b);
   }

   ChunkPos getColumnPos() {
      return (ChunkPos)Preconditions.checkNotNull(this.chunkPos);
   }

   public static class Handler extends AbstractClientMessageHandler<PacketUnloadColumn> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketUnloadColumn message, MessageContext ctx) {
         ICubicWorld worldClient = (ICubicWorld)world;
         if (worldClient.isCubicWorld()) {
            CubeProviderClient cubeCache = (CubeProviderClient)worldClient.getCubeCache();
            ChunkPos chunkPos = message.getColumnPos();
            cubeCache.func_73234_b(chunkPos.field_77276_a, chunkPos.field_77275_b);
         }
      }
   }
}
