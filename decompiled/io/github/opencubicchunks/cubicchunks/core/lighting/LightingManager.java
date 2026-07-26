package io.github.opencubicchunks.cubicchunks.core.lighting;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.FastCubeBlockAccess;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LightingManager implements ILightingManager {
   public static final boolean NO_SUNLIGHT_PROPAGATION = "true".equalsIgnoreCase(System.getProperty("cubicchunks.nosunlight"));
   public static final int MAX_CLIENT_LIGHT_SCAN_DEPTH = 64;
   @Nonnull
   private World world;
   @Nonnull
   private LightPropagator lightPropagator = new LightPropagator();
   @Nonnull
   private final List<LightingManager.IHeightChangeListener> heightUpdateListeners = new ArrayList<>();
   @Nullable
   private LightUpdateTracker tracker;
   @Nonnull
   private final Set<LightingManager.CubeLightUpdateInfo> toUpdate = new HashSet<>();

   public LightingManager(World world) {
      this.world = world;
   }

   @Nullable
   LightUpdateTracker getTracker() {
      if (NO_SUNLIGHT_PROPAGATION) {
         return null;
      } else {
         if (this.tracker == null && !this.world.field_72995_K && this.world.field_73011_w.func_191066_m()) {
            this.tracker = new LightUpdateTracker((PlayerCubeMap)((WorldServer)this.world).func_184164_w());
         }

         return this.tracker;
      }
   }

   public void registerHeightChangeListener(LightingManager.IHeightChangeListener listener) {
      this.heightUpdateListeners.add(listener);
   }

   @Nullable
   public LightingManager.CubeLightUpdateInfo createCubeLightUpdateInfo(Cube cube) {
      if (NO_SUNLIGHT_PROPAGATION) {
         return null;
      } else {
         return !cube.getWorld().field_73011_w.func_191066_m() ? null : new LightingManager.CubeLightUpdateInfo(cube, this);
      }
   }

   private void columnSkylightUpdate(LightingManager.UpdateType type, Chunk column, int localX, int minY, int maxY, int localZ) {
      if (!NO_SUNLIGHT_PROPAGATION) {
         if (this.world.field_73011_w.func_191066_m()) {
            int blockX = Coords.localToBlock(column.field_76635_g, localX);
            int blockZ = Coords.localToBlock(column.field_76647_h, localZ);
            if (type == LightingManager.UpdateType.IMMEDIATE) {
               TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
               TIntIterator it = toDiffuse.iterator();

               while (it.hasNext()) {
                  int cubeY = it.next();
                  ICube cube = ((IColumn)column).getCube(cubeY);
                  boolean success = this.updateDiffuseLight(cube, localX, localZ, minY, maxY);
                  if (!success) {
                     this.markCubeBlockColumnForUpdate(cube, blockX, blockZ);
                  }
               }
            } else {
               assert type == LightingManager.UpdateType.QUEUED;

               TIntSet toDiffuse = SkyLightUpdateCubeSelector.getCubesY(column, localX, localZ, minY, maxY);
               TIntIterator it = toDiffuse.iterator();

               while (it.hasNext()) {
                  int cubeY = it.next();
                  this.markCubeBlockColumnForUpdate(((IColumn)column).getCube(cubeY), blockX, blockZ);
               }
            }
         }
      }
   }

   private boolean updateDiffuseLight(ICube cube, int localX, int localZ, int minY, int maxY) {
      int minCubeY = cube.getCoords().getMinBlockY();
      int maxCubeY = cube.getCoords().getMaxBlockY();
      int minInCubeY = MathHelper.func_76125_a(minY, minCubeY, maxCubeY);
      int maxInCubeY = MathHelper.func_76125_a(maxY, minCubeY, maxCubeY);
      if (minInCubeY > maxInCubeY) {
         return true;
      } else {
         int blockX = Coords.localToBlock(cube.getX(), localX);
         int blockZ = Coords.localToBlock(cube.getZ(), localZ);
         return this.relightMultiBlock(new BlockPos(blockX, minInCubeY, blockZ), new BlockPos(blockX, maxInCubeY, blockZ), EnumSkyBlock.SKY, pos -> {
            this.world.func_175679_n(pos);
            LightUpdateTracker tracker = this.getTracker();
            if (tracker != null) {
               tracker.onUpdate(pos);
            }
         });
      }
   }

   @Override
   public void doOnBlockSetLightUpdates(Chunk column, int localX, int y1, int y2, int localZ) {
      this.columnSkylightUpdate(LightingManager.UpdateType.IMMEDIATE, column, localX, Math.min(y1, y2), Math.max(y1, y2), localZ);
   }

   @Override
   public void onTick() {
      Set<LightingManager.CubeLightUpdateInfo> updateSet = new HashSet<>(this.toUpdate);
      this.toUpdate.clear();
      int total = updateSet.size();
      long ms = -System.currentTimeMillis();
      Iterator<LightingManager.CubeLightUpdateInfo> iterator = updateSet.iterator();

      while (iterator.hasNext()) {
         LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = iterator.next();
         cubeLightUpdateInfo.tick();
         if (!cubeLightUpdateInfo.hasUpdates()) {
            iterator.remove();
         }
      }

      ms += System.currentTimeMillis();
      int updated = total - updateSet.size();
      if (ms > 50L) {
         CubicChunks.LOGGER.debug("Light tick: " + total + " cubes, " + updated + " updated in " + ms + "ms, " + (double)ms / (double)updated + "ms/cube");
      }

      this.toUpdate.addAll(updateSet);
      LightUpdateTracker tracker = this.getTracker();
      if (tracker != null) {
         tracker.sendAll();
      }
   }

   @Override
   public void markCubeBlockColumnForUpdate(ICube cube, int blockX, int blockZ) {
      LightingManager.CubeLightUpdateInfo data = ((Cube)cube).getCubeLightUpdateInfo();
      if (data != null) {
         data.markBlockColumnForUpdate(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
      }
   }

   @Override
   public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
      if (!this.world.func_175667_e(pos)) {
         return false;
      } else {
         ILightBlockAccess blocks = FastCubeBlockAccess.forBlockRegion(
            (ICubeProviderInternal)this.world.func_72863_F(), pos.func_177982_a(-17, -17, -17), pos.func_177982_a(17, 17, 17)
         );
         LightUpdateTracker tracker = this.getTracker();
         this.lightPropagator.propagateLight(pos, Collections.singleton(pos), blocks, lightType, updated -> {
            this.world.func_175679_n(updated);
            if (tracker != null) {
               tracker.onUpdate(updated);
            }
         });
         return true;
      }
   }

   private void markToUpdate(LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo) {
      this.toUpdate.add(cubeLightUpdateInfo);
   }

   boolean relightMultiBlock(BlockPos startPos, BlockPos endPos, EnumSkyBlock type, Consumer<BlockPos> notify) {
      if (NO_SUNLIGHT_PROPAGATION) {
         return true;
      } else {
         int LOAD_RADIUS = 17;
         BlockPos midPos = Coords.midPos(startPos, endPos);
         BlockPos minLoad = startPos.func_177982_a(-17, -17, -17);
         BlockPos maxLoad = endPos.func_177982_a(17, 17, 17);
         ILightBlockAccess blocks = FastCubeBlockAccess.forBlockRegion((ICubeProviderInternal)this.world.func_72863_F(), minLoad, maxLoad);
         this.lightPropagator.propagateLight(midPos, BlockPos.func_177980_a(startPos, endPos), blocks, type, notify);
         return true;
      }
   }

   public void sendHeightMapUpdate(BlockPos pos) {
      int size = this.heightUpdateListeners.size();

      for (int i = 0; i < size; i++) {
         this.heightUpdateListeners.get(i).heightUpdated(pos.func_177958_n(), pos.func_177952_p());
      }
   }

   public static class CubeLightUpdateInfo {
      private final Cube cube;
      private final boolean[] toUpdateColumns = new boolean[256];
      private final LightingManager lightingManager;
      private boolean hasUpdates;
      public EnumSet<EnumFacing> edgeNeedSkyLightUpdate = EnumSet.noneOf(EnumFacing.class);

      public CubeLightUpdateInfo(Cube cube, LightingManager lm) {
         this.cube = cube;
         this.lightingManager = lm;
      }

      void markBlockColumnForUpdate(int localX, int localZ) {
         this.toUpdateColumns[this.index(localX, localZ)] = true;
         this.hasUpdates = true;
         this.lightingManager.markToUpdate(this);
      }

      public void markEdgeNeedSkyLightUpdate(EnumFacing side) {
         this.edgeNeedSkyLightUpdate.add(side);
         this.lightingManager.markToUpdate(this);
      }

      public void tick() {
         if (!LightingManager.NO_SUNLIGHT_PROPAGATION) {
            ICubicWorldInternal cubicWorld = this.cube.getWorld();
            LightingManager manager = cubicWorld.getLightingManager();
            LightUpdateTracker tracker = manager.getTracker();
            ICubeProviderInternal cache = cubicWorld.getCubeCache();
            if (!this.edgeNeedSkyLightUpdate.isEmpty() && this.cube.getWorld().func_175648_a(this.cube.getCoords().getCenterBlockPos(), 16, false)) {
               EnumSet<EnumFacing> removed = EnumSet.noneOf(EnumFacing.class);

               for (EnumFacing dir : EnumFacing.values()) {
                  if (this.edgeNeedSkyLightUpdate.contains(dir)) {
                     CubePos cpos = this.cube.getCoords();
                     Cube loadedCube = cache.getLoadedCube(cpos.getX() + dir.func_82601_c(), cpos.getY() + dir.func_96559_d(), cpos.getZ() + dir.func_82599_e());
                     if (loadedCube != null && loadedCube.isInitialLightingDone()) {
                        int minX = cpos.getMinBlockX();
                        int minY = cpos.getMinBlockY();
                        int minZ = cpos.getMinBlockZ();
                        int maxX = cpos.getMaxBlockX();
                        int maxY = cpos.getMaxBlockY();
                        int maxZ = cpos.getMaxBlockZ();
                        switch (dir) {
                           case DOWN:
                              maxY = --minY + 1;
                              break;
                           case UP:
                              maxY++;
                              minY = maxY - 1;
                              break;
                           case NORTH:
                              maxZ = --minZ + 1;
                              break;
                           case SOUTH:
                              maxZ++;
                              minZ = maxZ - 1;
                              break;
                           case WEST:
                              maxX = --minX + 1;
                              break;
                           case EAST:
                              maxX++;
                              minX = maxX - 1;
                        }

                        manager.relightMultiBlock(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), EnumSkyBlock.SKY, pos -> {
                           this.cube.getWorld().func_175679_n(pos);
                           if (tracker != null) {
                              tracker.onUpdate(pos);
                           }
                        });
                        removed.add(dir);
                     }
                  }
               }

               for (EnumFacing dirx : removed) {
                  this.edgeNeedSkyLightUpdate.remove(dirx);
                  CubePos cpos = this.cube.getCoords();
                  Cube loadedCube = cache.getLoadedCube(cpos.getX() + dirx.func_82601_c(), cpos.getY() + dirx.func_96559_d(), cpos.getZ() + dirx.func_82599_e());

                  assert loadedCube != null;

                  LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = loadedCube.getCubeLightUpdateInfo();
                  if (cubeLightUpdateInfo != null) {
                     cubeLightUpdateInfo.edgeNeedSkyLightUpdate.remove(dirx.func_176734_d());
                  }
               }
            }

            if (this.hasUpdates) {
               for (int localX = 0; localX < 16; localX++) {
                  for (int localZ = 0; localZ < 16; localZ++) {
                     if (this.toUpdateColumns[this.index(localX, localZ)]) {
                        manager.relightMultiBlock(
                           new BlockPos(
                              Coords.localToBlock(this.cube.getX(), localX),
                              Coords.cubeToMinBlock(this.cube.getY()),
                              Coords.localToBlock(this.cube.getZ(), localZ)
                           ),
                           new BlockPos(
                              Coords.localToBlock(this.cube.getX(), localX),
                              Coords.cubeToMaxBlock(this.cube.getY()),
                              Coords.localToBlock(this.cube.getZ(), localZ)
                           ),
                           EnumSkyBlock.SKY,
                           pos -> {
                              this.cube.getWorld().func_175679_n(pos);
                              if (tracker != null) {
                                 tracker.onUpdate(pos);
                              }
                           }
                        );
                        this.toUpdateColumns[this.index(localX, localZ)] = false;
                     }
                  }
               }

               this.hasUpdates = false;
            }
         }
      }

      private int index(int x, int z) {
         return x << 4 | z;
      }

      public boolean hasUpdates() {
         return this.hasUpdates || !this.edgeNeedSkyLightUpdate.isEmpty();
      }

      public void clear() {
         for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
               this.toUpdateColumns[this.index(localX, localZ)] = false;
            }
         }

         this.hasUpdates = false;
      }

      public void onUnload() {
         this.lightingManager.toUpdate.remove(this);
      }
   }

   public interface IHeightChangeListener {
      void heightUpdated(int var1, int var2);
   }

   private static enum UpdateType {
      IMMEDIATE,
      QUEUED;

      private UpdateType() {
      }
   }
}
