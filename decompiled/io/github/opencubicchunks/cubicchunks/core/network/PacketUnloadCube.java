package io.github.opencubicchunks.cubicchunks.core.network;

import com.google.common.base.Preconditions;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketUnloadCube implements IMessage {
   private CubePos cubePos;

   public PacketUnloadCube() {
   }

   public PacketUnloadCube(CubePos cubePos) {
      this.cubePos = cubePos;
   }

   public void fromBytes(ByteBuf in) {
      this.cubePos = new CubePos(in.readInt(), in.readInt(), in.readInt());
   }

   public void toBytes(ByteBuf out) {
      out.writeInt(this.cubePos.getX());
      out.writeInt(this.cubePos.getY());
      out.writeInt(this.cubePos.getZ());
   }

   CubePos getCubePos() {
      return (CubePos)Preconditions.checkNotNull(this.cubePos);
   }

   public static class Handler extends AbstractClientMessageHandler<PacketUnloadCube> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketUnloadCube message, MessageContext ctx) {
         ICubicWorld worldClient = (ICubicWorld)world;
         if (worldClient.isCubicWorld()) {
            CubeProviderClient cubeCache = (CubeProviderClient)worldClient.getCubeCache();
            cubeCache.getCube(message.getCubePos()).markForRenderUpdate();
            cubeCache.unloadCube(message.getCubePos());
         }
      }
   }
}
