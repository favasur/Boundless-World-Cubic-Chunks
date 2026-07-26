package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityTracker.class})
public class MixinEntityTracker implements ICubicEntityTracker {
   @Shadow
   @Final
   private Set<EntityTrackerEntry> field_72793_b;
   private int maxVertTrackingDistanceThreshold;

   public MixinEntityTracker() {
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void onConstruct(WorldServer world, CallbackInfo ci) {
      this.setVertViewDistance(((ICubicPlayerList)world.func_73046_m().func_184103_al()).getVerticalViewDistance());
   }

   @Redirect(
      method = {"track(Lnet/minecraft/entity/Entity;IIZ)V"},
      at = @At(
         value = "NEW",
         target = "net/minecraft/entity/EntityTrackerEntry"
      )
   )
   private EntityTrackerEntry onCreateEntry(Entity entityIn, int rangeIn, int maxRangeIn, int updateFrequencyIn, boolean sendVelocityUpdatesIn) {
      EntityTrackerEntry e = new EntityTrackerEntry(entityIn, rangeIn, maxRangeIn, updateFrequencyIn, sendVelocityUpdatesIn);
      ((ICubicEntityTracker.Entry)e).setMaxVertRange(this.maxVertTrackingDistanceThreshold);
      return e;
   }

   @Override
   public void sendLeashedEntitiesInCube(EntityPlayerMP player, ICube cubeIn) {
      for (EntityTrackerEntry entitytrackerentry : this.field_72793_b) {
         Entity entity = entitytrackerentry.func_187260_b();
         if (entity != player && entity.field_70176_ah == cubeIn.getX() && entity.field_70164_aj == cubeIn.getZ() && entity.field_70162_ai == cubeIn.getY()) {
            entitytrackerentry.func_73117_b(player);
            if (entity instanceof EntityLiving && ((EntityLiving)entity).func_110166_bE() != null) {
               player.field_71135_a.func_147359_a(new SPacketEntityAttach(entity, ((EntityLiving)entity).func_110166_bE()));
            }

            if (!entity.func_184188_bt().isEmpty()) {
               player.field_71135_a.func_147359_a(new SPacketSetPassengers(entity));
            }
         }
      }
   }

   @Override
   public void setVertViewDistance(int viewDistance) {
      this.maxVertTrackingDistanceThreshold = (viewDistance - 1) * 16;

      for (EntityTrackerEntry e : this.field_72793_b) {
         ((ICubicEntityTracker.Entry)e).setMaxVertRange(this.maxVertTrackingDistanceThreshold);
      }
   }
}
