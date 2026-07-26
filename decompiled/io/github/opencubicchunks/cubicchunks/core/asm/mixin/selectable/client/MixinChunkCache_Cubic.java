package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client;

import io.github.opencubicchunks.cubicchunks.core.world.ICubicChunkCache;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ChunkCache.class})
public abstract class MixinChunkCache_Cubic implements ICubicChunkCache {
   @Shadow
   public World field_72815_e;

   public MixinChunkCache_Cubic() {
   }

   @Inject(
      method = {"getBiome"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBiome(BlockPos pos, CallbackInfoReturnable<Biome> cir) {
      if (this.isCubic()) {
         Cube cube = this.getCube(pos);
         if (cube == null) {
            cir.setReturnValue(Biomes.field_76772_c);
         } else {
            cir.setReturnValue(cube.getBiome(pos));
         }

         cir.cancel();
      }
   }
}
