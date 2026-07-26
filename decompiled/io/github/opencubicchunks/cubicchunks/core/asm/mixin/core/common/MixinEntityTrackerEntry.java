package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityTrackerEntry.class})
public class MixinEntityTrackerEntry implements ICubicEntityTracker.Entry {
   @Shadow
   @Final
   private int field_73130_b;
   @Shadow
   private long field_73129_e;
   @Shadow
   @Final
   private Entity field_73132_a;
   private int maxVertRange;

   public MixinEntityTrackerEntry() {
   }

   @Inject(
      method = {"isVisibleTo"},
      cancellable = true,
      at = {@At("RETURN")}
   )
   private void isVisibleToCubic(EntityPlayerMP player, CallbackInfoReturnable<Boolean> cir) {
      boolean ret = cir.getReturnValue();
      if (ret && ((ICubicWorld)player.field_70170_p).isCubicWorld()) {
         int rangeY = Math.min(this.field_73130_b, this.maxVertRange);
         double dy = player.field_70163_u - (double)this.field_73129_e / 4096.0;
         cir.setReturnValue(dy >= (double)(-rangeY) && dy <= (double)rangeY);
         cir.cancel();
      }
   }

   @Inject(
      method = {"isPlayerWatchingThisChunk"},
      cancellable = true,
      at = {@At("HEAD")}
   )
   private void isPlayerWatchingThisChunkCubic(EntityPlayerMP player, CallbackInfoReturnable<Boolean> cir) {
      if (((ICubicWorld)player.field_70170_p).isCubicWorld()) {
         boolean ret = ((PlayerCubeMap)player.func_71121_q().func_184164_w())
            .isPlayerWatchingCube(player, this.field_73132_a.field_70176_ah, this.field_73132_a.field_70162_ai, this.field_73132_a.field_70164_aj);
         cir.setReturnValue(ret);
         cir.cancel();
      }
   }

   @Override
   public void setMaxVertRange(int maxVertTrackingDistanceThreshold) {
      this.maxVertRange = maxVertTrackingDistanceThreshold;
   }
}
