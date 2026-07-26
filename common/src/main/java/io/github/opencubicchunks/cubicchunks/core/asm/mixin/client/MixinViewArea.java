package io.github.opencubicchunks.cubicchunks.core.asm.mixin.client;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.MixinViewFrustum_RenderHeightFix
@Mixin(ViewArea.class)
public abstract class MixinViewArea {

    @Shadow
    protected Level level;
    @Shadow
    protected int sectionGridSizeX;
    @Shadow
    protected int sectionGridSizeY;
    @Shadow
    protected int sectionGridSizeZ;
    @Shadow
    public SectionRenderDispatcher.RenderSection[] sections;

    @Shadow
    protected abstract int getSectionIndex(int x, int y, int z);

    @Inject(method = "setViewDistance(I)V", at = @At("TAIL"))
    private void cc$setViewDistance(int dist, CallbackInfo ci) {
        if (this.level != null && ((ICubicWorldInternal) this.level).isCubicWorld()) {
            // Use a cubic grid sized by the horizontal render distance so the renderer can
            // wrap sections vertically as the camera moves.
            this.sectionGridSizeY = this.sectionGridSizeX;
        }
    }

    @Inject(method = "repositionCamera(DD)V", at = @At("HEAD"), cancellable = true)
    private void cc$repositionCamera(double x, double z, CallbackInfo ci) {
        if (this.level == null || !((ICubicWorldInternal) this.level).isCubicWorld()) {
            return;
        }
        ci.cancel();

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        int i = Mth.ceil(camX);
        int j = Mth.ceil(camY);
        int k = Mth.ceil(camZ);

        for (int xIndex = 0; xIndex < this.sectionGridSizeX; xIndex++) {
            int lX = this.sectionGridSizeX * 16;
            int baseX = i - 8 - lX / 2;
            int originX = baseX + Math.floorMod(xIndex * 16 - baseX, lX);

            for (int zIndex = 0; zIndex < this.sectionGridSizeZ; zIndex++) {
                int lZ = this.sectionGridSizeZ * 16;
                int baseZ = k - 8 - lZ / 2;
                int originZ = baseZ + Math.floorMod(zIndex * 16 - baseZ, lZ);

                for (int yIndex = 0; yIndex < this.sectionGridSizeY; yIndex++) {
                    int lY = this.sectionGridSizeY * 16;
                    int baseY = j - 8 - lY / 2;
                    int originY = baseY + Math.floorMod(yIndex * 16 - baseY, lY);

                    SectionRenderDispatcher.RenderSection section = this.sections[this.getSectionIndex(xIndex, yIndex, zIndex)];
                    BlockPos pos = section.getOrigin();
                    if (originX != pos.getX() || originY != pos.getY() || originZ != pos.getZ()) {
                        section.setOrigin(originX, originY, originZ);
                    }
                }
            }
        }
    }

    @Inject(method = "getRenderSectionAt", at = @At("HEAD"), cancellable = true)
    private void cc$getRenderSectionAt(BlockPos pos, CallbackInfoReturnable<SectionRenderDispatcher.RenderSection> cir) {
        if (this.level == null || !((ICubicWorldInternal) this.level).isCubicWorld()) {
            return;
        }
        int sx = Mth.positiveModulo(Mth.floorDiv(pos.getX(), 16), this.sectionGridSizeX);
        int sy = Mth.positiveModulo(Mth.floorDiv(pos.getY(), 16) - this.level.getMinSection(), this.sectionGridSizeY);
        int sz = Mth.positiveModulo(Mth.floorDiv(pos.getZ(), 16), this.sectionGridSizeZ);
        cir.setReturnValue(this.sections[this.getSectionIndex(sx, sy, sz)]);
    }
}
