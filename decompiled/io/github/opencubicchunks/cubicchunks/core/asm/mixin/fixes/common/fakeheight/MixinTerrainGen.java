package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight;

import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import java.util.Random;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({TerrainGen.class})
public class MixinTerrainGen {
   public MixinTerrainGen() {
   }

   @Overwrite(
      remap = false
   )
   public static boolean decorate(World world, Random rand, ChunkPos chunkPos, EventType type) {
      Decorate event = new Decorate(world, rand, chunkPos, null, type);
      CompatHandler.postBiomeDecorateWithFakeWorldHeight(event);
      return event.getResult() != Result.DENY;
   }
}
