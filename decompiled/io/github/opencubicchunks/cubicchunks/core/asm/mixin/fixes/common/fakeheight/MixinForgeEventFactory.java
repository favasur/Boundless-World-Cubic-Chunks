package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight;

import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Post;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Pre;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({ForgeEventFactory.class})
public class MixinForgeEventFactory {
   public MixinForgeEventFactory() {
   }

   @Overwrite(
      remap = false
   )
   public static void onChunkPopulate(boolean pre, IChunkGenerator gen, World world, Random rand, int x, int z, boolean hasVillageGenerated) {
      if (pre) {
         CompatHandler.postChunkPopulatePreWithFakeWorldHeight(new Pre(gen, world, rand, x, z, hasVillageGenerated));
      } else {
         MinecraftForge.EVENT_BUS.post(new Post(gen, world, rand, x, z, hasVillageGenerated));
      }
   }
}
