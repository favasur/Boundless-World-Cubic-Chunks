package io.github.opencubicchunks.cubicchunks.core.asm.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.ChunkBorderRenderer;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla 2D chunk borders with 3D cubic-cube grid outlines when the
 * player presses F3+G. Injects into {@link ChunkBorderRenderer#render} which
 * is dedicated to F3+G border display — no manual toggle check needed.
 */
@Mixin(ChunkBorderRenderer.class)
public class MixinDebugRenderChunkBorder {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cc$renderCubeBorders(
            PoseStack poseStack, MultiBufferSource bufferSource,
            double camX, double camY, double camZ,
            CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || !((ICubicWorld) level).isCubicWorld()) {
            return;
        }

        int renderDist = mc.options.renderDistance().get();
        int playerCubeX = (int) Math.floor(camX / 16.0);
        int playerCubeY = (int) Math.floor(camY / 16.0);
        int playerCubeZ = (int) Math.floor(camZ / 16.0);

        // Limit vertical range to ±6 cubes (96 blocks) — full render distance
        // in Y would draw ~15k cubes and tank fps
        int vertRadius = Math.min(renderDist, 6);
        int minCX = playerCubeX - renderDist;
        int maxCX = playerCubeX + renderDist;
        int minCY = playerCubeY - vertRadius;
        int maxCY = playerCubeY + vertRadius;
        int minCZ = playerCubeZ - renderDist;
        int maxCZ = playerCubeZ + renderDist;

        // Use lines() for independent line pairs (GL_LINES) instead of
        // debugLineStrip() which would produce ghost connecting lines
        VertexConsumer thin = bufferSource.getBuffer(RenderType.lines());
        VertexConsumer thick = bufferSource.getBuffer(RenderType.lines());

        Matrix4f matrix = poseStack.last().pose();

        // Thin yellow sub-grid: every 2 blocks within each cube
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                float x1 = cx * 16.0f, x2 = x1 + 16.0f;
                float z1 = cz * 16.0f, z2 = z1 + 16.0f;
                float y0 = minCY * 16.0f;
                float y1 = (maxCY + 1) * 16.0f;
                for (int k = 0; k <= 16; k += 2) {
                    float vx = x1 + k;
                    thin.addVertex(matrix, vx, y0, z1).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, vx, y1, z1).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, vx, y0, z2).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, vx, y1, z2).setColor(1, 1, 0, 0.4f);
                    float vz = z1 + k;
                    thin.addVertex(matrix, x1, y0, vz).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, x1, y1, vz).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, x2, y0, vz).setColor(1, 1, 0, 0.4f);
                    thin.addVertex(matrix, x2, y1, vz).setColor(1, 1, 0, 0.4f);
                }
            }
        }

        // Thick blue cube boundaries (vertical edges + horizontal planes)
        for (int cx = minCX; cx <= maxCX + 1; cx++) {
            for (int cz = minCZ; cz <= maxCZ + 1; cz++) {
                float x = cx * 16.0f, z = cz * 16.0f;
                float y0 = minCY * 16.0f;
                float y1 = (maxCY + 1) * 16.0f;
                thick.addVertex(matrix, x, y0, z).setColor(0.25f, 0.25f, 1, 0.6f);
                thick.addVertex(matrix, x, y1, z).setColor(0.25f, 0.25f, 1, 0.6f);
            }
        }
        for (int cy = minCY; cy <= maxCY + 1; cy++) {
            float y = cy * 16.0f;
            for (int cx = minCX; cx <= maxCX; cx++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    float x1 = cx * 16.0f, x2 = x1 + 16.0f;
                    float z1 = cz * 16.0f, z2 = z1 + 16.0f;
                    // Bottom of horizontal platform (each cube floor)
                    thick.addVertex(matrix, x1, y, z1).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x2, y, z1).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x1, y, z1).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x1, y, z2).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x2, y, z2).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x2, y, z1).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x2, y, z2).setColor(0.25f, 0.25f, 1, 0.6f);
                    thick.addVertex(matrix, x1, y, z2).setColor(0.25f, 0.25f, 1, 0.6f);
                }
            }
        }

        ci.cancel(); // Replace vanilla chunk borders with our cubic grid
    }
}
