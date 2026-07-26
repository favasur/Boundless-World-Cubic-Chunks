package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.core.asm.optifine.ChunkPos3;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ViewFrustum.class})
public class MixinViewFrustum {
   public MixinViewFrustum() {
   }

   @Redirect(
      method = {"updateVboRegion(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V"},
      at = @At(
         value = "NEW",
         target = "net/minecraft/util/math/ChunkPos"
      )
   )
   @Dynamic
   private ChunkPos getChunkPos(int x, int z, RenderChunk renderChunk) {
      return new ChunkPos3(x, renderChunk.func_178568_j().func_177956_o() & -256, z);
   }
}
