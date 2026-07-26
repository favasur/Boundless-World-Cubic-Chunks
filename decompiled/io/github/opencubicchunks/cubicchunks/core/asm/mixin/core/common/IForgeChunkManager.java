package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {ForgeChunkManager.class},
   remap = false
)
public interface IForgeChunkManager {
   @Accessor
   static Map<World, Multimap<String, Ticket>> getTickets() {
      throw new Error("IForgeChunkManager failed to apply");
   }

   @Accessor
   static SetMultimap<String, Ticket> getPlayerTickets() {
      throw new Error("IForgeChunkManager failed to apply");
   }
}
