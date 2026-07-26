package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({WorldClient.class})
@Implements({@Interface(
      iface = ICubicWorldInternal.Client.class,
      prefix = "world$"
   )})
public abstract class MixinWorldClient extends MixinWorld implements ICubicWorldInternal.Client {
   @Shadow
   private ChunkProviderClient field_73033_b;

   public MixinWorldClient() {
   }

   @Override
   public void initCubicWorldClient(IntRange heightRange, IntRange generationRange) {
      super.initCubicWorld(heightRange, generationRange);
      this.isCubicWorld = true;
      CubeProviderClient cubeProviderClient = new CubeProviderClient(this);
      this.field_73020_y = cubeProviderClient;
      this.field_73033_b = cubeProviderClient;
   }

   @Override
   public void tickCubicWorld() {
      this.getLightingManager().onTick();
   }

   @Override
   public CubeProviderClient getCubeCache() {
      return (CubeProviderClient)this.field_73033_b;
   }

   @Override
   public void setHeightBounds(int minHeight1, int maxHeight1) {
      this.minHeight = minHeight1;
      this.maxHeight = maxHeight1;
   }
}
