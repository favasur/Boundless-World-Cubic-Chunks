package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {World.class},
   priority = 1001
)
public abstract class MixinWorld_CollisionCheck implements ICubicWorldInternal {
   public MixinWorld_CollisionCheck() {
   }

   @Shadow
   public abstract boolean func_189509_E(BlockPos var1);

   @Inject(
      method = {"getCollisionBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;ZLjava/util/List;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void addBlocksCollisionBoundingBoxesToList(
      @Nullable Entity entity, AxisAlignedBB aabb, boolean breakOnWorldBorder, @Nullable List<AxisAlignedBB> aabbList, CallbackInfoReturnable<Boolean> ci
   ) {
      if (this.isCubicWorld()) {
         double minX = aabb.field_72340_a;
         double minY = aabb.field_72338_b;
         double minZ = aabb.field_72339_c;
         double maxX = aabb.field_72336_d;
         double maxY = aabb.field_72337_e;
         double maxZ = aabb.field_72334_f;
         int x1 = MathHelper.func_76128_c(minX) - 1;
         int y1 = MathHelper.func_76128_c(minY) - 1;
         int z1 = MathHelper.func_76128_c(minZ) - 1;
         int x2 = MathHelper.func_76143_f(maxX);
         int y2 = MathHelper.func_76143_f(maxY);
         int z2 = MathHelper.func_76143_f(maxZ);
         PooledMutableBlockPos pooledmutableblockpos = PooledMutableBlockPos.func_185346_s();

         label148:
         for (int cx = Coords.blockToCube(x1); cx <= Coords.blockToCube(x2); cx++) {
            for (int cy = Coords.blockToCube(y1); cy <= Coords.blockToCube(y2); cy++) {
               for (int cz = Coords.blockToCube(z1); cz <= Coords.blockToCube(z2); cz++) {
                  CubePos coords = new CubePos(cx, cy, cz);
                  int minBlockX = coords.getMinBlockX();
                  int minBlockY = coords.getMinBlockY();
                  int minBlockZ = coords.getMinBlockZ();
                  int maxBlockX = coords.getMaxBlockX();
                  int maxBlockY = coords.getMaxBlockY();
                  int maxBlockZ = coords.getMaxBlockZ();
                  Cube loadedCube = this.getCubeCache().getLoadedCube(coords);
                  if (loadedCube != null && loadedCube.getStorage() != null) {
                     minBlockX = minBlockX > x1 ? minBlockX : x1;
                     minBlockY = minBlockY > y1 ? minBlockY : y1;
                     minBlockZ = minBlockZ > z1 ? minBlockZ : z1;
                     maxBlockX = maxBlockX < x2 ? maxBlockX : x2;
                     maxBlockY = maxBlockY < y2 ? maxBlockY : y2;
                     maxBlockZ = maxBlockZ < z2 ? maxBlockZ : z2;

                     for (int x = minBlockX; x <= maxBlockX; x++) {
                        boolean isXboundary = x == x1 || x == x2;

                        for (int z = minBlockZ; z <= maxBlockZ; z++) {
                           boolean isZboundary = z == z1 || z == z2;
                           if (!isXboundary || !isZboundary) {
                              for (int y = minBlockY; y <= maxBlockY; y++) {
                                 boolean isYboundary = y == y2;
                                 if (!isYboundary || !isZboundary && !isXboundary) {
                                    pooledmutableblockpos.func_181079_c(x, y, z);
                                    if (!this.func_189509_E(pooledmutableblockpos)) {
                                       IBlockState bstate = loadedCube.getStorage()
                                          .func_177485_a(Coords.blockToLocal(x), Coords.blockToLocal(y), Coords.blockToLocal(z));
                                       bstate.func_185908_a((World)this, pooledmutableblockpos, aabb, aabbList, entity, false);
                                       MinecraftForge.EVENT_BUS.post(new GetCollisionBoxesEvent((World)this, null, aabb, aabbList));
                                       if (breakOnWorldBorder && !aabbList.isEmpty()) {
                                          break label148;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         pooledmutableblockpos.func_185344_t();
         ci.setReturnValue(!aabbList.isEmpty());
         ci.cancel();
      }
   }
}
