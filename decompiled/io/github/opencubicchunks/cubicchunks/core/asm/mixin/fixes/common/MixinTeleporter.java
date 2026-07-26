package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({Teleporter.class})
public class MixinTeleporter {
   @Shadow
   @Final
   protected WorldServer field_85192_a;
   @Shadow
   @Final
   protected Random field_77187_a;

   public MixinTeleporter() {
   }

   @Redirect(
      method = {"placeInExistingPortal"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"
      ),
      slice = @Slice(
         from = @At(
            value = "NEW",
            target = "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;"
         ),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;down()Lnet/minecraft/util/math/BlockPos;"
         )
      )
   )
   private BlockPos makeTopStartPos(BlockPos orig, int dx, int dy, int dz, Entity entity, float rotationYaw) {
      return ((ICubicWorld)this.field_85192_a).isCubicWorld() ? orig.func_177982_a(dx, 128, dz) : orig.func_177982_a(dx, dy, dz);
   }

   @ModifyConstant(
      method = {"placeInExistingPortal"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO},
         ordinal = 1
      )}
   )
   private int getScanBottomY(int zero, Entity entity, float rotationYaw) {
      return ((ICubicWorld)this.field_85192_a).isCubicWorld() ? MathHelper.func_76128_c(entity.field_70163_u - 128.0) : zero;
   }

   @Overwrite
   public boolean func_85188_a(Entity entityIn) {
      double distanceToPortal = -1.0;
      int x = MathHelper.func_76128_c(entityIn.field_70165_t);
      int y = MathHelper.func_76128_c(entityIn.field_70163_u);
      int z = MathHelper.func_76128_c(entityIn.field_70161_v);
      int x1 = x;
      int y1 = y;
      int z1 = z;
      int searchFromY = 70;
      int searchToY = this.field_85192_a.func_72940_L() - 1;
      if (((ICubicWorld)this.field_85192_a).isCubicWorld()) {
         searchFromY = y - 128;
         searchToY = y + 128;
      }

      int verticalPlane = 0;
      int random = this.field_77187_a.nextInt(4);
      MutableBlockPos pos = new MutableBlockPos();

      for (int ix = x - 16; ix <= x + 16; ix++) {
         double dX = (double)ix + 0.5 - entityIn.field_70165_t;

         for (int iz = z - 16; iz <= z + 16; iz++) {
            double dZ = (double)iz + 0.5 - entityIn.field_70161_v;

            for (int minDepth = 3; minDepth >= 1; minDepth -= 2) {
               label203:
               for (int iy = searchToY; iy >= searchFromY; iy--) {
                  if (this.field_85192_a.func_175623_d(pos.func_181079_c(ix, iy, iz))) {
                     while (this.field_85192_a.func_175623_d(pos)) {
                        iy--;
                        pos.func_181079_c(ix, iy - 1, iz);
                        if (this.field_85192_a.func_189509_E(pos)) {
                           break;
                        }
                     }

                     for (int verticalPlaneSelector = random; verticalPlaneSelector < random + 4; verticalPlaneSelector++) {
                        int xPlane = verticalPlaneSelector % 2;
                        int zPlane = 1 - xPlane;
                        if (verticalPlaneSelector % 4 >= 2) {
                           xPlane = -xPlane;
                           zPlane = -zPlane;
                        }

                        for (int depth = 0; depth < minDepth; depth++) {
                           for (int width = 0; width < 4; width++) {
                              for (int height = -1; height < 4; height++) {
                                 int ix1 = ix + (width - 1) * xPlane + depth * zPlane;
                                 int iy1 = iy + height;
                                 int iz1 = iz + (width - 1) * zPlane - depth * xPlane;
                                 pos.func_181079_c(ix1, iy1, iz1);
                                 if (height < 0 && !this.field_85192_a.func_180495_p(pos).func_185904_a().func_76220_a()
                                    || height >= 0 && !this.field_85192_a.func_175623_d(pos)) {
                                    continue label203;
                                 }
                              }
                           }
                        }

                        double dY = (double)iy + 0.5 - entityIn.field_70163_u;
                        double distanceToPortal1 = dX * dX + dY * dY + dZ * dZ;
                        if (distanceToPortal < 0.0 || distanceToPortal1 < distanceToPortal) {
                           distanceToPortal = distanceToPortal1;
                           x1 = ix;
                           y1 = iy;
                           z1 = iz;
                           verticalPlane = verticalPlaneSelector % 4;
                        }
                     }
                  }
               }
            }
         }
      }

      int xPlanex = verticalPlane % 2;
      int zPlanex = 1 - xPlanex;
      if (verticalPlane % 4 >= 2) {
         xPlanex = -xPlanex;
         zPlanex = -zPlanex;
      }

      if (distanceToPortal < 0.0) {
         for (int depth = -1; depth <= 1; depth++) {
            for (int width = 1; width < 3; width++) {
               for (int heightx = -1; heightx < 3; heightx++) {
                  int ix = x1 + (width - 1) * xPlanex + depth * zPlanex;
                  int iyx = y1 + heightx;
                  int iz = z1 + (width - 1) * zPlanex - depth * xPlanex;
                  boolean isBase = heightx < 0;
                  this.field_85192_a
                     .func_175656_a(new BlockPos(ix, iyx, iz), isBase ? Blocks.field_150343_Z.func_176223_P() : Blocks.field_150350_a.func_176223_P());
               }
            }
         }
      }

      IBlockState portal = Blocks.field_150427_aO.func_176223_P().func_177226_a(BlockPortal.field_176550_a, xPlanex == 0 ? Axis.Z : Axis.X);

      for (int width = 0; width < 4; width++) {
         for (int heightx = -1; heightx < 4; heightx++) {
            int ix = x1 + (width - 1) * xPlanex;
            int iyx = y1 + heightx;
            int iz = z1 + (width - 1) * zPlanex;
            boolean isFrame = width == 0 || width == 3 || heightx == -1 || heightx == 3;
            this.field_85192_a.func_180501_a(new BlockPos(ix, iyx, iz), isFrame ? Blocks.field_150343_Z.func_176223_P() : portal, 2);
         }
      }

      for (int width = 0; width < 4; width++) {
         for (int heightx = -1; heightx < 4; heightx++) {
            int ix = x1 + (width - 1) * xPlanex;
            int iyx = y1 + heightx;
            int iz = z1 + (width - 1) * zPlanex;
            BlockPos blockpos = new BlockPos(ix, iyx, iz);
            this.field_85192_a.func_175685_c(blockpos, this.field_85192_a.func_180495_p(blockpos).func_177230_c(), false);
         }
      }

      return true;
   }
}
