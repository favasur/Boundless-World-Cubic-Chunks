package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.INetHandlerPlayClient;
import io.github.opencubicchunks.cubicchunks.core.util.SideUtils;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractMessageHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {
   public AbstractMessageHandler() {
   }

   public abstract void handleClientMessage(World var1, EntityPlayer var2, T var3, MessageContext var4);

   public abstract void handleServerMessage(EntityPlayer var1, T var2, MessageContext var3);

   @Nullable
   public final IMessage onMessage(T message, MessageContext ctx) {
      try {
         IThreadListener taskQueue = SideUtils.getForSide(
            () -> () -> Minecraft.func_71410_x(), () -> () -> FMLCommonHandler.instance().getMinecraftServerInstance()
         );
         if (!taskQueue.func_152345_ab()) {
            taskQueue.func_152344_a(() -> this.onMessage(message, ctx));
            return null;
         } else {
            World mainWorld = SideUtils.getForSide(
               () -> AbstractMessageHandler.ClientAccessProxy::getWorld, () -> () -> FMLCommonHandler.instance().getMinecraftServerInstance().func_71218_a(0)
            );
            if (mainWorld == null) {
               CubicChunks.LOGGER.warn("Received packet when world doesn't exist!");
               return null;
            } else {
               EntityPlayer player = SideUtils.getForSide(
                  ctx, () -> AbstractMessageHandler.ClientAccessProxy::getPlayer, () -> c -> c.getServerHandler().field_147369_b
               );
               if (ctx.side.isClient()) {
                  this.handleClientMessage(mainWorld, player, message, ctx);
               } else {
                  this.handleServerMessage(player, message, ctx);
               }

               return null;
            }
         }
      } catch (Throwable var6) {
         CubicChunks.LOGGER.catching(var6);
         FMLCommonHandler.instance().exitJava(-1, false);
         throw var6;
      }
   }

   private static class ClientAccessProxy {
      private ClientAccessProxy() {
      }

      static EntityPlayer getPlayer(MessageContext c) {
         return (EntityPlayer)(c.side.isClient() ? Minecraft.func_71410_x().field_71439_g : c.getServerHandler().field_147369_b);
      }

      @Nullable
      static World getWorld() {
         return Minecraft.func_71410_x().func_147114_u() == null ? null : ((INetHandlerPlayClient)Minecraft.func_71410_x().func_147114_u()).getWorld();
      }
   }
}
