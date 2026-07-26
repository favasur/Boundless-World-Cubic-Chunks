package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderChunk.class})
public abstract class MixinRenderChunk implements IOptifineRenderChunk {
   @Shadow
   @Final
   private MutableBlockPos field_178586_f;
   @Shadow
   private World field_178588_d;
   @Shadow(
      remap = false
   )
   @Dynamic
   private RenderChunk[] renderChunkNeighboursValid;
   @Shadow(
      remap = false
   )
   @Dynamic
   private RenderChunk[] renderChunkNeighbours;
   private int regionY;
   @Shadow(
      remap = false
   )
   @Dynamic
   private int regionX;
   private ICube cube;
   private boolean isCubic;

   public MixinRenderChunk() {
   }

   @Shadow
   public abstract BlockPos func_178568_j();

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onConstruct(World worldIn, RenderGlobal renderGlobalIn, int indexIn, CallbackInfo cbi) {
      this.isCubic = ((ICubicWorld)worldIn).isCubicWorld();
   }

   @Inject(
      method = {"setPosition"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;chunk:Lnet/minecraft/world/chunk/Chunk;",
         remap = false
      )},
      remap = true
   )
   @Dynamic
   private void onSetChunk(int x, int y, int z, CallbackInfo cbi) {
      this.cube = null;
      this.isCubic = ((ICubicWorld)this.field_178588_d).isCubicWorld();
      this.regionY = y & -256;
   }

   @Inject(
      method = {"updateRenderChunkNeighboursValid()V"},
      at = {@At("HEAD")},
      remap = false
   )
   @Dynamic
   private void onUpdateNeighbors(CallbackInfo cbi) {
      if (this.isCubic) {
         int y = this.func_178568_j().func_177956_o();
         int up = EnumFacing.UP.ordinal();
         int down = EnumFacing.DOWN.ordinal();
         this.renderChunkNeighboursValid[up] = this.renderChunkNeighbours[up].func_178568_j().func_177956_o() == y + 16 ? this.renderChunkNeighbours[up] : null;
         this.renderChunkNeighboursValid[down] = this.renderChunkNeighbours[down].func_178568_j().func_177956_o() == y - 16
            ? this.renderChunkNeighbours[down]
            : null;
      }
   }

   @ModifyArg(
      method = {"preRenderBlocks"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/BufferBuilder;setTranslation(DDD)V",
         ordinal = 0
      ),
      index = 1
   )
   @Dynamic
   private double getRegionY(double dy) {
      return dy;
   }

   @Override
   public ICube getCube() {
      return this.getCube(this.field_178586_f);
   }

   @Override
   public boolean isCubic() {
      return this.isCubic;
   }

   @Override
   public int getRegionY() {
      return this.regionY;
   }

   @Override
   public int getRegionX() {
      return this.regionX;
   }

   private ICube getCube(BlockPos posIn) {
      ICube cubeLocal = this.cube;
      if (cubeLocal != null && cubeLocal.isCubeLoaded()) {
         return cubeLocal;
      } else {
         cubeLocal = ((ICubicWorld)this.field_178588_d).getCubeFromBlockCoords(posIn);
         this.cube = cubeLocal;
         return cubeLocal;
      }
   }
}
