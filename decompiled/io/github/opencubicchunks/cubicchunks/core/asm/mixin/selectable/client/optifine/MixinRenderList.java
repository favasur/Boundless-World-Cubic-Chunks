package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RenderList.class, VboRenderList.class})
public abstract class MixinRenderList extends ChunkRenderContainer {
   @Shadow(
      remap = false
   )
   @Dynamic
   private double viewEntityY;
   private int renderChunkLayer_regionY = Integer.MIN_VALUE;

   public MixinRenderList() {
   }

   @ModifyConstant(
      method = {"renderChunkLayer"},
      constant = {@Constant(
         intValue = Integer.MIN_VALUE,
         ordinal = 0
      )}
   )
   @Dynamic
   private int initRegionX(int orig) {
      this.renderChunkLayer_regionY = orig;
      return orig;
   }

   @Redirect(
      method = {"renderChunkLayer"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;regionX:I",
         ordinal = 0
      ),
      remap = false
   )
   @Dynamic
   private int getHackedRegionX(RenderChunk rc) {
      int regionY = this.renderChunkLayer_regionY;
      int rcRegY = ((IOptifineRenderChunk)rc).getRegionY();
      return regionY != rcRegY ? 1 : ((IOptifineRenderChunk)rc).getRegionX();
   }

   @ModifyArg(
      method = {"preRenderRegion"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"
      ),
      index = 1,
      remap = false,
      require = 0
   )
   @Group(
      name = "preRenderRegion",
      min = 1,
      max = 2
   )
   @Dynamic
   private float drawRegionRedirect_deobf(float zero) {
      return (float)((double)this.renderChunkLayer_regionY - this.viewEntityY);
   }

   @ModifyArg(
      method = {"preRenderRegion"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GlStateManager;func_179109_b(FFF)V"
      ),
      index = 1,
      remap = false,
      require = 0
   )
   @Group(
      name = "preRenderRegion",
      min = 1,
      max = 2
   )
   @Dynamic
   private float drawRegionRedirect_obf(float zero) {
      return (float)((double)this.renderChunkLayer_regionY - this.viewEntityY);
   }

   @Redirect(
      method = {"renderChunkLayer"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;regionX:I",
         ordinal = 1
      ),
      remap = false
   )
   @Dynamic
   private int updateRegionY(RenderChunk rc) {
      this.renderChunkLayer_regionY = ((IOptifineRenderChunk)rc).getRegionY();
      return ((IOptifineRenderChunk)rc).getRegionX();
   }
}
