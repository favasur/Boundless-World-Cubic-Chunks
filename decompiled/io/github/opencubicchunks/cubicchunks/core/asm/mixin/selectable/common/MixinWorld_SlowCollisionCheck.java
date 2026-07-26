package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({World.class})
public abstract class MixinWorld_SlowCollisionCheck implements ICubicWorld {
   public MixinWorld_SlowCollisionCheck() {
   }

   @Shadow
   public abstract boolean func_191503_g(Entity var1);

   @Shadow
   public abstract WorldBorder func_175723_af();

   @Shadow
   public abstract boolean func_175667_e(BlockPos var1);

   @Shadow
   public abstract IBlockState func_180495_p(BlockPos var1);

   @Overwrite(
      constraints = "MC_FORGE(23)"
   )
   private boolean func_191504_a(@Nullable Entity entity, AxisAlignedBB aabb, boolean flagArg, @Nullable List<AxisAlignedBB> aabbList) {
      int minX = MathHelper.func_76128_c(aabb.field_72340_a) - 1;
      int maxX = MathHelper.func_76143_f(aabb.field_72336_d) + 1;
      int minY = MathHelper.func_76128_c(aabb.field_72338_b) - 1;
      int maxY = MathHelper.func_76143_f(aabb.field_72337_e) + 1;
      int minZ = MathHelper.func_76128_c(aabb.field_72339_c) - 1;
      int maxZ = MathHelper.func_76143_f(aabb.field_72334_f) + 1;
      WorldBorder worldborder = this.func_175723_af();
      boolean entityOutsideOfBorder = entity != null && entity.func_174832_aS();
      boolean entityInsideOfBorder = entity != null && this.func_191503_g(entity);
      IBlockState iblockstate = Blocks.field_150348_b.func_176223_P();
      PooledMutableBlockPos pos = PooledMutableBlockPos.func_185346_s();

      try {
         for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
               boolean isXboundary = x == minX || x == maxX - 1;
               boolean isZBoundary = z == minZ || z == maxZ - 1;
               if ((!isXboundary || !isZBoundary) && this.isBlockColumnLoaded(pos.func_181079_c(x, 64, z))) {
                  for (int y = minY; y < maxY; y++) {
                     if ((!isXboundary && !isZBoundary || y != maxY - 1) && this.func_175667_e(pos.func_181079_c(x, y, z))) {
                        if (flagArg) {
                           if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                              return true;
                           }
                        } else if (entity != null && entityOutsideOfBorder == entityInsideOfBorder) {
                           entity.func_174821_h(!entityInsideOfBorder);
                        }

                        pos.func_181079_c(x, y, z);
                        IBlockState iblockstate1;
                        if (!flagArg && !worldborder.func_177746_a(pos) && entityInsideOfBorder) {
                           iblockstate1 = iblockstate;
                        } else {
                           iblockstate1 = this.func_180495_p(pos);
                        }

                        iblockstate1.func_185908_a((World)this, pos, aabb, aabbList, entity, false);
                        MinecraftForge.EVENT_BUS.post(new GetCollisionBoxesEvent((World)this, null, aabb, aabbList));
                        if (flagArg && !aabbList.isEmpty()) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      } finally {
         pos.func_185344_t();
      }

      return !aabbList.isEmpty();
   }
}
