package io.github.opencubicchunks.cubicchunks.core.network;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketDispatcher {
   private static byte packetId = 0;
   private static final SimpleNetworkWrapper dispatcher = NetworkRegistry.INSTANCE.newSimpleChannel("cubicchunks");

   public PacketDispatcher() {
   }

   public static void registerPackets() {
      registerMessage(PacketCubes.Handler.class, PacketCubes.class);
      registerMessage(PacketColumn.Handler.class, PacketColumn.class);
      registerMessage(PacketUnloadColumn.Handler.class, PacketUnloadColumn.class);
      registerMessage(PacketUnloadCube.Handler.class, PacketUnloadCube.class);
      registerMessage(PacketCubeBlockChange.Handler.class, PacketCubeBlockChange.class);
      registerMessage(PacketCubicWorldData.Handler.class, PacketCubicWorldData.class);
      registerMessage(PacketHeightMapUpdate.Handler.class, PacketHeightMapUpdate.class);
      registerMessage(PacketCubeSkyLightUpdates.Handler.class, PacketCubeSkyLightUpdates.class);
   }

   private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(
      @Nonnull Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, Class<REQ> messageClass
   ) {
      Side side = AbstractClientMessageHandler.class.isAssignableFrom(handlerClass) ? Side.CLIENT : Side.SERVER;
      dispatcher.registerMessage(handlerClass, messageClass, packetId++, side);
   }

   public static void sendTo(IMessage message, EntityPlayerMP player) {
      dispatcher.sendTo(message, player);
   }
}
