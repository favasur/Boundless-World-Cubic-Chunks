package io.github.opencubicchunks.cubicchunks.core.network;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractServerMessageHandler<T extends IMessage> extends AbstractMessageHandler<T> {
   public AbstractServerMessageHandler() {
   }

   @Override
   public final void handleClientMessage(World world, EntityPlayer player, T message, MessageContext ctx) {
   }
}
