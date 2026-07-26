package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({World.class})
public abstract class MixinWorld_Tick implements ICubicWorld {
   @Shadow
   @Final
   public boolean field_72995_K;
   private int updateEntity_entityPosY;
   private int updateEntity_entityPosX;
   private int updateEntity_entityPosZ;

   public MixinWorld_Tick() {
   }

   @Shadow
   private boolean func_175663_a(int x1, int y1, int z1, int x2, int y2, int z2, boolean allowEmpty) {
      throw new Error();
   }

   @Shadow
   public abstract boolean func_175701_a(BlockPos var1);

   @Redirect(
      method = {"updateEntityWithOptionalForce"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isAreaLoaded(IIIIIIZ)Z"
      ),
      require = 1
   )
   @Group(
      name = "updateEntity",
      max = 2,
      min = 2
   )
   private boolean canUpdateEntity(
      World _this, int startBlockX, int oldStartBlockY, int startBlockZ, int endBlockX, int oldEndBlockY, int endBlockZ, boolean allowEmpty
   ) {
      if (!this.isCubicWorld()) {
         return this.func_175663_a(startBlockX, oldStartBlockY, startBlockZ, endBlockX, oldEndBlockY, endBlockZ, allowEmpty);
      } else {
         BlockPos entityPos = new BlockPos(this.updateEntity_entityPosX, this.updateEntity_entityPosY, this.updateEntity_entityPosZ);
         if (!this.func_175701_a(entityPos)) {
            return true;
         } else {
            int r = endBlockX - startBlockX >> 1;
            return this.func_175663_a(
               this.updateEntity_entityPosX - r,
               this.updateEntity_entityPosY - r,
               this.updateEntity_entityPosZ - r,
               this.updateEntity_entityPosX + r,
               this.updateEntity_entityPosY + r,
               this.updateEntity_entityPosZ + r,
               true
            );
         }
      }
   }

   @Inject(
      method = {"updateEntityWithOptionalForce"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getPersistentChunks()Lcom/google/common/collect/ImmutableSetMultimap;",
         remap = false
      )},
      require = 1
   )
   @Group(
      name = "updateEntity"
   )
   public void onIsAreaLoadedForUpdateEntityWithOptionalForce(Entity entity, boolean force, CallbackInfo ci) {
      this.updateEntity_entityPosY = MathHelper.func_76128_c(entity.field_70163_u);
      this.updateEntity_entityPosX = MathHelper.func_76128_c(entity.field_70165_t);
      this.updateEntity_entityPosZ = MathHelper.func_76128_c(entity.field_70161_v);
   }
}
