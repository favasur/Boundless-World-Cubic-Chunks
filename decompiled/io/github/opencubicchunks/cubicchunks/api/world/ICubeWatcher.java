package io.github.opencubicchunks.cubicchunks.api.world;

import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeWatcher extends XYZAddressable {
   boolean isSentToPlayers();

   @Nullable
   ICube getCube();

   void sendPacketToAllPlayers(IMessage var1);

   @Override
   int getX();

   @Override
   int getY();

   @Override
   int getZ();

   boolean shouldTick();
}
