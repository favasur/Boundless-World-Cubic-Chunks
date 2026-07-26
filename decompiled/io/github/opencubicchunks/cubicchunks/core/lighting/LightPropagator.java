package io.github.opencubicchunks.cubicchunks.core.lighting;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.EnumSkyBlock;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightPropagator {
   @Nonnull
   private LightUpdateQueue internalRelightQueue = new LightUpdateQueue();

   public LightPropagator() {
   }

   public void propagateLight(
      BlockPos centerPos, Iterable<? extends BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback
   ) {
      this.propagateLight(centerPos, coords, blocks, type, true, setLightCallback);
   }

   public void propagateLight(
      BlockPos centerPos,
      Iterable<? extends BlockPos> coords,
      ILightBlockAccess blocks,
      EnumSkyBlock type,
      boolean handleDecreased,
      Consumer<BlockPos> setLightCallback
   ) {
      if (type != EnumSkyBlock.SKY || !LightingManager.NO_SUNLIGHT_PROPAGATION) {
         this.internalRelightQueue.begin(centerPos);

         try {
            if (!CubicChunksConfig.fastSimplifiedSkyLight || type != EnumSkyBlock.SKY) {
               if (handleDecreased) {
                  this.queueDecreasedLights(coords, blocks, type);
                  this.handleDecreasedLights(blocks, type, setLightCallback);
                  this.internalRelightQueue.resetIndex();
               }

               this.queueIncreasedLights(coords, blocks, type, setLightCallback);
               this.handleLightSpread(blocks, type, setLightCallback);
               return;
            }

            this.doFastSimplifiedSkylight(coords, blocks, type, setLightCallback);
         } catch (Throwable var16) {
            CrashReport report = CrashReport.func_85055_a(var16, "Updating skylight");
            CrashReportCategory category = report.func_85058_a("Skylight update");
            category.func_189529_a("CenterLocation", () -> CrashReportCategory.func_180522_a(centerPos));
            int i = 0;

            for (BlockPos pos : coords) {
               category.func_189529_a("UpdateLocation" + i, () -> CrashReportCategory.func_180522_a(pos));
               i++;
            }

            throw new ReportedException(report);
         } finally {
            this.internalRelightQueue.end();
         }
      }
   }

   private void queueDecreasedLights(Iterable<? extends BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type) {
      for (BlockPos coord : coords) {
         int emitted = blocks.getEmittedLight(coord, type);
         if (blocks.getLightFor(type, coord) > emitted) {
            this.internalRelightQueue.put(coord, emitted, LightUpdateQueue.MAX_DISTANCE);
         }
      }
   }

   private void handleDecreasedLights(ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
      MutableBlockPos scratchPos = new MutableBlockPos();
      MutableBlockPos scratchPos2 = new MutableBlockPos();

      while (this.internalRelightQueue.next()) {
         BlockPos pos = scratchPos.func_181079_c(this.internalRelightQueue.getX(), this.internalRelightQueue.getY(), this.internalRelightQueue.getZ());
         int distance = this.internalRelightQueue.getDistance();
         int currentValue = blocks.getLightFor(type, pos);
         int lightFromNeighbors = this.getExpectedLight(blocks, type, pos, scratchPos2);
         if (lightFromNeighbors <= currentValue - 1) {
            if (!blocks.setLightFor(type, pos, 0)) {
               this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
            } else {
               setLightCallback.accept(pos);

               for (EnumFacing direction : EnumFacing.values()) {
                  BlockPos offset = pos.func_177972_a(direction);
                  if (!blocks.hasNeighborsAccessible(offset)) {
                     this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
                  } else {
                     this.internalRelightQueue.put(offset, blocks.getEmittedLight(offset, type), distance - 1);
                  }
               }
            }
         }
      }
   }

   private void queueIncreasedLights(Iterable<? extends BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
      MutableBlockPos scratchPos = new MutableBlockPos();

      for (BlockPos coord : coords) {
         int emitted = this.getExpectedLight(blocks, type, coord, scratchPos);
         if (emitted > blocks.getLightFor(type, coord)) {
            this.internalRelightQueue.put(coord, emitted, LightUpdateQueue.MAX_DISTANCE);
            if (blocks.setLightFor(type, coord, emitted)) {
               setLightCallback.accept(coord);
            } else {
               this.markNeighborEdgeNeedLightUpdate(coord, blocks, type);
            }
         }
      }
   }

   private void handleLightSpread(ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
      MutableBlockPos scratchPos = new MutableBlockPos();
      MutableBlockPos scratchPos2 = new MutableBlockPos();
      MutableBlockPos scratchPos3 = new MutableBlockPos();

      while (this.internalRelightQueue.next()) {
         BlockPos pos = scratchPos.func_181079_c(this.internalRelightQueue.getX(), this.internalRelightQueue.getY(), this.internalRelightQueue.getZ());
         int distance = this.internalRelightQueue.isBeforeReset() ? LightUpdateQueue.MAX_DISTANCE : this.internalRelightQueue.getDistance();

         for (EnumFacing direction : EnumFacing.values()) {
            scratchPos2.func_189533_g(pos);
            scratchPos2.func_189536_c(direction);
            if (!blocks.hasNeighborsAccessible(scratchPos2)) {
               this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
            } else {
               int newLight = this.getExpectedLight(blocks, type, scratchPos2, scratchPos3);
               if (newLight > blocks.getLightFor(type, scratchPos2) && blocks.getEmittedLight(scratchPos2, type) < newLight) {
                  if (blocks.setLightFor(type, scratchPos2, newLight)) {
                     setLightCallback.accept(scratchPos2);
                     if (distance - 1 > 0) {
                        this.internalRelightQueue.put(scratchPos2, newLight, distance - 1);
                     }
                  } else {
                     this.markNeighborEdgeNeedLightUpdate(pos, blocks, type);
                  }
               }
            }
         }
      }
   }

   private void doFastSimplifiedSkylight(Iterable<? extends BlockPos> coords, ILightBlockAccess blocks, EnumSkyBlock type, Consumer<BlockPos> setLightCallback) {
      for (BlockPos coord : coords) {
         int max = blocks.getEmittedLight(coord, type);
         if (max >= 11) {
            blocks.setLightFor(type, coord, max);
         } else {
            int opacity = blocks.getBlockLightOpacity(coord);
            if (opacity < 15) {
               EnumFacing[] var9 = EnumFacing.field_82609_l;
               int var10 = var9.length;
               int var11 = 0;

               while (true) {
                  if (var11 < var10) {
                     EnumFacing value = var9[var11];
                     max = Math.max(max, blocks.getEmittedLight(coord.func_177972_a(value), type) - Math.max(1, opacity) * 4);
                     if (max < 11) {
                        var11++;
                        continue;
                     }
                  }

                  blocks.setLightFor(type, coord, Math.max(7, max));
                  setLightCallback.accept(coord);
                  break;
               }
            }
         }
      }
   }

   private int getExpectedLight(ILightBlockAccess blocks, EnumSkyBlock type, BlockPos pos, MutableBlockPos scratchPos) {
      int emittedLight = blocks.getEmittedLight(pos, type);
      return emittedLight >= 15 ? 15 : Math.max(emittedLight, blocks.getLightFromNeighbors(type, pos, scratchPos));
   }

   private void markNeighborEdgeNeedLightUpdate(BlockPos pos, ILightBlockAccess blocks, EnumSkyBlock type) {
      for (EnumFacing direction : EnumFacing.values()) {
         BlockPos offset = pos.func_177972_a(direction);
         blocks.markEdgeNeedLightUpdate(offset, type);
      }
   }
}
