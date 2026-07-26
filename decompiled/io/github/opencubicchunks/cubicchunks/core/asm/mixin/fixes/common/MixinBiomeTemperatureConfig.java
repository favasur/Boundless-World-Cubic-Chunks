package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Biome.class})
public abstract class MixinBiomeTemperatureConfig {
   @Shadow
   @Final
   protected static NoiseGeneratorPerlin field_150605_ac;

   public MixinBiomeTemperatureConfig() {
   }

   @Shadow
   public abstract float func_185353_n();

   @Overwrite
   public float func_180626_a(BlockPos pos) {
      if (pos.func_177956_o() > CubicChunksConfig.biomeTemperatureCenterY) {
         float noise = (float)(field_150605_ac.func_151601_a((double)((float)pos.func_177958_n() / 8.0F), (double)((float)pos.func_177952_p() / 8.0F)) * 4.0);
         int y = Math.min(pos.func_177956_o(), CubicChunksConfig.biomeTemperatureScaleMaxY);
         return this.func_185353_n() + (noise + (float)y - (float)CubicChunksConfig.biomeTemperatureCenterY) * CubicChunksConfig.biomeTemperatureHeightFactor;
      } else {
         return this.func_185353_n();
      }
   }
}
