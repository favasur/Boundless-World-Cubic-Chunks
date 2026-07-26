package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ViewFrustum.class})
public class MixinViewFrustum_RenderHeightFix {
   @Shadow
   @Final
   protected World field_178167_b;
   @Shadow
   public RenderChunk[] field_178164_f;
   @Shadow
   protected int field_178165_d;
   @Shadow
   protected int field_178168_c;
   @Shadow
   protected int field_178166_e;

   public MixinViewFrustum_RenderHeightFix() {
   }

   @Shadow
   private int func_178157_a(int arg1, int arg2, int arg3) {
      throw new Error();
   }

   @Inject(
      method = {"updateChunkPositions"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void updateChunkPositionsInject(double viewEntityX, double viewEntityZ, CallbackInfo cbi) {
      if (((ICubicWorld)this.field_178167_b).isCubicWorld()) {
         Entity view = Minecraft.func_71410_x().func_175606_aa();
         double x = view.field_70165_t;
         double y = view.field_70163_u;
         double z = view.field_70161_v;
         int viewX = MathHelper.func_76128_c(x) - 8;
         int viewY = MathHelper.func_76128_c(y) - 8;
         int viewZ = MathHelper.func_76128_c(z) - 8;
         int xSizeInBlocks = this.field_178165_d * 16;
         int ySizeInBlocks = this.field_178168_c * 16;
         int zSizeInBlocks = this.field_178166_e * 16;

         for (int xIndex = 0; xIndex < this.field_178165_d; xIndex++) {
            int blockX = this.func_178157_a(viewX, xSizeInBlocks, xIndex);

            for (int yIndex = 0; yIndex < this.field_178168_c; yIndex++) {
               int blockY = this.func_178157_a(viewY, ySizeInBlocks, yIndex);

               for (int zIndex = 0; zIndex < this.field_178166_e; zIndex++) {
                  int blockZ = this.func_178157_a(viewZ, zSizeInBlocks, zIndex);
                  int rendererIndex = (zIndex * this.field_178168_c + yIndex) * this.field_178165_d + xIndex;
                  RenderChunk renderer = this.field_178164_f[rendererIndex];
                  BlockPos oldPos = renderer.func_178568_j();
                  if (oldPos.func_177958_n() != blockX || oldPos.func_177956_o() != blockY || oldPos.func_177952_p() != blockZ) {
                     renderer.func_189562_a(blockX, blockY, blockZ);
                  }
               }
            }
         }

         cbi.cancel();
      }
   }

   @Inject(
      method = {"getRenderChunk"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void getRenderChunkInject(BlockPos pos, CallbackInfoReturnable<RenderChunk> cbi) {
      if (((ICubicWorld)this.field_178167_b).isCubicWorld()) {
         int x = MathHelper.func_76137_a(pos.func_177958_n(), 16);
         int y = MathHelper.func_76137_a(pos.func_177956_o(), 16);
         int z = MathHelper.func_76137_a(pos.func_177952_p(), 16);
         x %= this.field_178165_d;
         if (x < 0) {
            x += this.field_178165_d;
         }

         y %= this.field_178168_c;
         if (y < 0) {
            y += this.field_178168_c;
         }

         z %= this.field_178166_e;
         if (z < 0) {
            z += this.field_178166_e;
         }

         int index = (z * this.field_178168_c + y) * this.field_178165_d + x;
         RenderChunk renderChunk = this.field_178164_f[index];
         cbi.cancel();
         cbi.setReturnValue(renderChunk);
      }
   }
}
