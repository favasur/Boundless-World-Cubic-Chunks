package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.RenderCubeCache;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({RenderChunk.class})
public class MixinRenderChunk {
   @Shadow
   private World field_178588_d;

   public MixinRenderChunk() {
   }

   @Inject(
      method = {"createRegionRenderCache"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   protected void createCubicChunkCache(World world, BlockPos from, BlockPos to, int subtract, CallbackInfoReturnable<ChunkCache> cbi) {
      if (((ICubicWorld)world).isCubicWorld()) {
         cbi.setReturnValue(new RenderCubeCache(world, from, to, subtract));
         cbi.cancel();
      }
   }
}
