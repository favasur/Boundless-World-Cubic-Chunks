package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ChunkRenderWorker.class})
public abstract class MixinRenderWorker {
   public MixinRenderWorker() {
   }

   @Redirect(
      method = {"processTask"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderWorker;isChunkExisting(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;)Z",
         ordinal = 0
      )
   )
   private boolean onIsChunkExisting(ChunkRenderWorker chunkRenderWorker, BlockPos pos, World world) {
      MutableBlockPos p = (MutableBlockPos)pos;
      if (((ICubicWorld)world).isCubicWorld()) {
         if (!this.func_188263_a(p.func_189534_c(EnumFacing.EAST, 16).func_189534_c(EnumFacing.DOWN, 16), world)) {
            return false;
         }

         if (!this.func_188263_a(p.func_189534_c(EnumFacing.UP, 32), world)) {
            return false;
         }

         p.func_189534_c(EnumFacing.DOWN, 16).func_189534_c(EnumFacing.WEST, 16);
      }

      return this.func_188263_a(p, world);
   }

   @Overwrite
   private boolean func_188263_a(BlockPos pos, World worldIn) {
      return ((ICubicWorld)worldIn).isCubicWorld()
         ? ((ICubicWorld)worldIn).getCubeCache().getLoadedCube(pos.func_177958_n() >> 4, pos.func_177956_o() >> 4, pos.func_177952_p() >> 4) != null
         : !worldIn.func_72964_e(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4).func_76621_g();
   }
}
