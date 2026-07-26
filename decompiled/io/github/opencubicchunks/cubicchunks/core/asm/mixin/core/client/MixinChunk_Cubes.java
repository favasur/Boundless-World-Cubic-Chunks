package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({Chunk.class})
public abstract class MixinChunk_Cubes implements IColumn {
   @Shadow
   @Final
   private ExtendedBlockStorage[] field_76652_q;
   private CubeMap cubeMap;
   private IHeightMap opacityIndex;
   private Cube cachedCube;
   private boolean isColumn = false;

   public MixinChunk_Cubes() {
   }

   @Shadow
   public abstract ExtendedBlockStorage[] func_76587_i();

   @Inject(
      method = {"generateHeightMap"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void generateHeightMap_CubicChunks_Cancel(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Inject(
      method = {"read"},
      at = {@At("HEAD")}
   )
   private void fillChunk_CubicChunks_NotSupported(PacketBuffer buf, int i, boolean flag, CallbackInfo cbi) {
   }
}
