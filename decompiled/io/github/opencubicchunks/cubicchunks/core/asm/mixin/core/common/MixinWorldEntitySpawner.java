package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.core.world.IWorldEntitySpawner;
import javax.annotation.Nullable;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WorldEntitySpawner.class})
public class MixinWorldEntitySpawner implements IWorldEntitySpawner.Handler {
   @Nullable
   private IWorldEntitySpawner customSpawner;

   public MixinWorldEntitySpawner() {
   }

   @Override
   public void setEntitySpawner(@Nullable IWorldEntitySpawner spawner) {
      this.customSpawner = spawner;
   }

   @Nullable
   @Override
   public IWorldEntitySpawner getEntitySpawner() {
      return this.customSpawner;
   }

   @Inject(
      method = {"findChunksForSpawning"},
      cancellable = true,
      at = {@At("HEAD")}
   )
   private void onSpawnMobs(WorldServer world, boolean hostileEnable, boolean peacefulEnable, boolean spawnOnSetTickRate, CallbackInfoReturnable<Integer> cir) {
      if (this.customSpawner != null) {
         int ret = this.customSpawner.findChunksForSpawning(world, hostileEnable, peacefulEnable, spawnOnSetTickRate);
         cir.setReturnValue(ret);
         cir.cancel();
      }
   }
}
