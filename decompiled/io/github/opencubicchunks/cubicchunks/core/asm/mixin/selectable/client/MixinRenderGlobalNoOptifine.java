package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IViewFrustum;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({RenderGlobal.class})
public class MixinRenderGlobalNoOptifine {
   @Nullable
   private BlockPos position;
   @Shadow
   private int field_72739_F;
   @Shadow
   private ViewFrustum field_175008_n;

   public MixinRenderGlobalNoOptifine() {
   }

   @Nullable
   @Overwrite
   private RenderChunk func_181562_a(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
      BlockPos blockpos = renderChunkBase.func_181701_a(facing);
      return MathHelper.func_76130_a(playerPos.func_177958_n() - blockpos.func_177958_n()) > this.field_72739_F * 16
         ? null
         : (
            MathHelper.func_76130_a(playerPos.func_177956_o() - blockpos.func_177956_o()) > this.field_72739_F * 16
               ? null
               : (
                  MathHelper.func_76130_a(playerPos.func_177952_p() - blockpos.func_177952_p()) > this.field_72739_F * 16
                     ? null
                     : ((IViewFrustum)this.field_175008_n).getRenderChunkAt(blockpos)
               )
         );
   }
}
