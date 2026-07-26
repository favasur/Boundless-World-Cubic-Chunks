package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({RegionFileCache.class})
public class MixinRegionFileCache {
   public MixinRegionFileCache() {
   }

   @Inject(
      method = {"clearRegionFileReferences"},
      at = {@At("HEAD")}
   )
   private static void onClearRefs(CallbackInfo cbi) throws IOException {
      SharedCachedRegionProvider.clearRegions();
   }
}
