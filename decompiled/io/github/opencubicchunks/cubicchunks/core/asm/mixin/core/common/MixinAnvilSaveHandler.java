package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.server.CubicAnvilChunkLoader;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import java.io.File;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({AnvilSaveHandler.class})
public class MixinAnvilSaveHandler {
   public MixinAnvilSaveHandler() {
   }

   @Redirect(
      method = {"getChunkLoader"},
      at = @At(
         value = "NEW",
         target = "net/minecraft/world/chunk/storage/AnvilChunkLoader"
      )
   )
   public AnvilChunkLoader getChunkLoader(File file, DataFixer dataFixer, WorldProvider provider) {
      ICubicWorld world = (ICubicWorld)((ICubicWorldProvider)provider).getWorld();
      return (AnvilChunkLoader)(world.isCubicWorld()
         ? new CubicAnvilChunkLoader(file, dataFixer, () -> ((ICubeProviderInternal.Server)world.getCubeCache()).getCubeIO())
         : new AnvilChunkLoader(file, dataFixer));
   }
}
