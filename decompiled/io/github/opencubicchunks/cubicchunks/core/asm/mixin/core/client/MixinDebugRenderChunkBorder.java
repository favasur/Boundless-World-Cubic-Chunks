package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.debug.DebugRendererChunkBorder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DebugRendererChunkBorder.class})
public class MixinDebugRenderChunkBorder {
   public MixinDebugRenderChunkBorder() {
   }

   private boolean isCubicWorld() {
      return ((ICubicWorld)Minecraft.func_71410_x().field_71441_e).isCubicWorld();
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderChunkBorder(float partialTicks, long finishTimeNano, CallbackInfo ci) {
      if (this.isCubicWorld()) {
         ci.cancel();
         EntityPlayer player = Minecraft.func_71410_x().field_71439_g;
         Tessellator tessellator = Tessellator.func_178181_a();
         BufferBuilder bufferbuilder = tessellator.func_178180_c();
         double playerX = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * (double)partialTicks;
         double playerY = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * (double)partialTicks;
         double playerZ = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * (double)partialTicks;
         double yOffset = (double)(Math.round(playerY / 16.0) * 16L - 128L);
         double minY = 0.0 - playerY + yOffset;
         double maxY = 256.0 - playerY + yOffset;
         GlStateManager.func_179090_x();
         GlStateManager.func_179084_k();
         double chunkX = (double)(player.field_70176_ah << 4) - playerX;
         double chunkZ = (double)(player.field_70164_aj << 4) - playerZ;
         GlStateManager.func_187441_d(1.0F);
         bufferbuilder.func_181668_a(3, DefaultVertexFormats.field_181706_f);

         for (int i = -16; i <= 32; i += 16) {
            for (int j = -16; j <= 32; j += 16) {
               bufferbuilder.func_181662_b(chunkX + (double)i, minY, chunkZ + (double)j).func_181666_a(1.0F, 0.0F, 0.0F, 0.0F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)i, minY, chunkZ + (double)j).func_181666_a(1.0F, 0.0F, 0.0F, 0.5F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)i, maxY, chunkZ + (double)j).func_181666_a(1.0F, 0.0F, 0.0F, 0.5F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)i, maxY, chunkZ + (double)j).func_181666_a(1.0F, 0.0F, 0.0F, 0.0F).func_181675_d();
            }
         }

         for (int k = 2; k < 16; k += 2) {
            bufferbuilder.func_181662_b(chunkX + (double)k, minY, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, minY, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, maxY, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, maxY, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, minY, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, minY, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, maxY, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + (double)k, maxY, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
         }

         for (int l = 2; l < 16; l += 2) {
            bufferbuilder.func_181662_b(chunkX, minY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, minY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, maxY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, maxY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, minY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, minY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, maxY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, maxY, chunkZ + (double)l).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
         }

         for (int i1 = (int)yOffset; i1 <= 256 + (int)yOffset; i1 += 2) {
            double d7 = (double)i1 - playerY;
            bufferbuilder.func_181662_b(chunkX, d7, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d7, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d7, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, d7, chunkZ + 16.0).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, d7, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d7, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d7, chunkZ).func_181666_a(1.0F, 1.0F, 0.0F, 0.0F).func_181675_d();
         }

         tessellator.func_78381_a();
         GlStateManager.func_187441_d(2.0F);
         bufferbuilder.func_181668_a(3, DefaultVertexFormats.field_181706_f);

         for (int j1 = 0; j1 <= 16; j1 += 16) {
            for (int l1 = 0; l1 <= 16; l1 += 16) {
               bufferbuilder.func_181662_b(chunkX + (double)j1, minY, chunkZ + (double)l1).func_181666_a(0.25F, 0.25F, 1.0F, 0.0F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)j1, minY, chunkZ + (double)l1).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)j1, maxY, chunkZ + (double)l1).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
               bufferbuilder.func_181662_b(chunkX + (double)j1, maxY, chunkZ + (double)l1).func_181666_a(0.25F, 0.25F, 1.0F, 0.0F).func_181675_d();
            }
         }

         for (int k1 = (int)yOffset; k1 <= 256 + (int)yOffset; k1 += 16) {
            double d8 = (double)k1 - playerY;
            bufferbuilder.func_181662_b(chunkX, d8, chunkZ).func_181666_a(0.25F, 0.25F, 1.0F, 0.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d8, chunkZ).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d8, chunkZ + 16.0).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, d8, chunkZ + 16.0).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX + 16.0, d8, chunkZ).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d8, chunkZ).func_181666_a(0.25F, 0.25F, 1.0F, 1.0F).func_181675_d();
            bufferbuilder.func_181662_b(chunkX, d8, chunkZ).func_181666_a(0.25F, 0.25F, 1.0F, 0.0F).func_181675_d();
         }

         tessellator.func_78381_a();
         GlStateManager.func_187441_d(1.0F);
         GlStateManager.func_179147_l();
         GlStateManager.func_179098_w();
      }
   }
}
