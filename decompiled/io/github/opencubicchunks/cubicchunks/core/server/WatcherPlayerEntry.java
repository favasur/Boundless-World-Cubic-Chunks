package io.github.opencubicchunks.cubicchunks.core.server;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class WatcherPlayerEntry {
   @Nonnull
   EntityPlayerMP player;

   WatcherPlayerEntry(EntityPlayerMP player) {
      this.player = player;
   }
}
