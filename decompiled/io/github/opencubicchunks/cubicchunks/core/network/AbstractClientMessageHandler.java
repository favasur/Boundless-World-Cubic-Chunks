package io.github.opencubicchunks.cubicchunks.core.network;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractClientMessageHandler<T extends IMessage> extends AbstractMessageHandler<T> {
   public AbstractClientMessageHandler() {
   }

   @Override
   public final void handleServerMessage(EntityPlayer player, T message, MessageContext ctx) {
   }
}
