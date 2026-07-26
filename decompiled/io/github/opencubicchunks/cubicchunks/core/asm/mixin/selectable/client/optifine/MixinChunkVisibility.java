package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"net.optifine.render.ChunkVisibility"}
)
public class MixinChunkVisibility {
   @Shadow(
      remap = false
   )
   @Dynamic
   private static int counter = 0;
   @Shadow(
      remap = false
   )
   @Dynamic
   private static int iMaxStatic = -1;
   @Shadow(
      remap = false
   )
   @Dynamic
   private static int iMaxStaticFinal = Coords.blockToCube(Integer.MAX_VALUE) - 1;
   @Shadow(
      remap = false
   )
   @Dynamic
   private static World worldLast = null;
   @Shadow(
      remap = false
   )
   @Dynamic
   private static int pcxLast = Integer.MIN_VALUE;
   private static int pcyLast = Integer.MIN_VALUE;
   @Shadow(
      remap = false
   )
   @Dynamic
   private static int pczLast = Integer.MIN_VALUE;

   public MixinChunkVisibility() {
   }

   @Inject(
      method = {"getMaxChunkY"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   @Dynamic
   private static void getMaxChunkYCC(World world, Entity viewEntity, int renderDistanceChunks, CallbackInfoReturnable<Integer> cbi) {
      if (((ICubicWorld)world).isCubicWorld()) {
         cbi.cancel();
         cbi.setReturnValue(2147483646);
      }
   }
}
