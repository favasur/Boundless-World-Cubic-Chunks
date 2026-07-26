package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({ChunkCache.class})
public abstract class MixinChunkCache_Vanilla {
   @Shadow
   public World field_72815_e;
   @Shadow
   protected int field_72818_a;
   @Shadow
   protected int field_72816_b;
   @Shadow
   protected Chunk[][] field_72817_c;

   public MixinChunkCache_Vanilla() {
   }

   @Shadow(
      remap = false
   )
   abstract boolean withinBounds(int var1, int var2);

   @Inject(
      method = {"getBiome"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBiome(BlockPos pos, CallbackInfoReturnable<Biome> cir) {
      ICubicWorld cworld = (ICubicWorld)this.field_72815_e;
      if (cworld.isCubicWorld()) {
         cir.cancel();
         int chunkX = Coords.blockToCube(pos.func_177958_n()) - this.field_72818_a;
         int chunkZ = Coords.blockToCube(pos.func_177952_p()) - this.field_72816_b;
         if (!this.withinBounds(chunkX, chunkZ)) {
            cir.setReturnValue(Biomes.field_76772_c);
         } else {
            ICube cube = ((IColumn)this.field_72817_c[chunkX][chunkZ]).getCube(Coords.blockToCube(pos.func_177956_o()));
            cir.setReturnValue(cube.getBiome(pos));
         }
      }
   }
}
