package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicChunkCache;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ChunkCache.class})
public class MixinChunkCache implements ICubicChunkCache {
   @Shadow
   public World field_72815_e;
   @Nonnull
   private Cube[][][] cubes;
   private int originX;
   private int originY;
   private int originZ;
   boolean isCubic = false;
   private int dx;
   private int dy;
   private int dz;
   private IBlockState air = Blocks.field_150350_a.func_176223_P();

   public MixinChunkCache() {
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void initChunkCache(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, CallbackInfo ci) {
      if (worldIn != null && ((ICubicWorld)worldIn).isCubicWorld() && worldIn.func_175624_G() instanceof ICubicWorldType) {
         this.isCubic = true;
         CubePos start = CubePos.fromBlockCoords(posFromIn.func_177982_a(-subIn, -subIn, -subIn));
         CubePos end = CubePos.fromBlockCoords(posToIn.func_177982_a(subIn, subIn, subIn));
         this.dx = Math.abs(end.getX() - start.getX()) + 1;
         this.dy = Math.abs(end.getY() - start.getY()) + 1;
         this.dz = Math.abs(end.getZ() - start.getZ()) + 1;
         ICubeProviderInternal prov = (ICubeProviderInternal)worldIn.func_72863_F();
         this.cubes = new Cube[this.dx][this.dy][this.dz];
         this.originX = Math.min(start.getX(), end.getX());
         this.originY = Math.min(start.getY(), end.getY());
         this.originZ = Math.min(start.getZ(), end.getZ());

         for (int relativeCubeX = 0; relativeCubeX < this.dx; relativeCubeX++) {
            for (int relativeCubeZ = 0; relativeCubeZ < this.dz; relativeCubeZ++) {
               for (int relativeCubeY = 0; relativeCubeY < this.dy; relativeCubeY++) {
                  Cube cube = prov.getCube(this.originX + relativeCubeX, this.originY + relativeCubeY, this.originZ + relativeCubeZ);
                  this.cubes[relativeCubeX][relativeCubeY][relativeCubeZ] = cube;
               }
            }
         }
      }
   }

   @Inject(
      method = {"getBlockState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> cir) {
      if (this.isCubic) {
         Cube cube = this.getCube(pos);
         if (cube == null) {
            cir.setReturnValue(this.air);
            cir.cancel();
         } else {
            cir.setReturnValue(cube.getBlockState(pos));
            cir.cancel();
         }
      }
   }

   @Nullable
   @Override
   public Cube getCube(BlockPos pos) {
      if (!this.isCubic) {
         return null;
      } else {
         int blockX = pos.func_177958_n();
         int blockY = pos.func_177956_o();
         int blockZ = pos.func_177952_p();
         int cubeX = Coords.blockToCube(blockX) - this.originX;
         int cubeY = Coords.blockToCube(blockY) - this.originY;
         int cubeZ = Coords.blockToCube(blockZ) - this.originZ;
         return cubeX >= 0 && cubeX < this.dx && cubeY >= 0 && cubeY < this.dy && cubeZ >= 0 && cubeZ < this.dz ? this.cubes[cubeX][cubeY][cubeZ] : null;
      }
   }

   @Override
   public boolean isCubic() {
      return this.isCubic;
   }
}
