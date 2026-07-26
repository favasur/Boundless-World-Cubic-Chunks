package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.StagingHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Collection;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(
   value = {Chunk.class},
   priority = 2000
)
@Implements({@Interface(
      iface = IColumn.class,
      prefix = "chunk$"
   )})
public abstract class MixinChunk_Column implements IColumn, IColumnInternal {
   private CubeMap cubeMap;
   private IHeightMap opacityIndex;
   private Cube cachedCube;
   private StagingHeightMap stagingHeightMap;
   private boolean isColumn;
   @Shadow
   @Final
   public int field_76647_h;
   @Shadow
   @Final
   public int field_76635_g;
   @Shadow
   @Final
   private World field_76637_e;
   @Shadow
   public boolean field_189550_d;
   @Shadow
   @Final
   private int[] field_76634_f;

   public MixinChunk_Column() {
   }

   public Cube getLoadedCube(int cubeY) {
      return this.cachedCube != null && this.cachedCube.getY() == cubeY
         ? this.cachedCube
         : ((ICubicWorldInternal)this.getWorld()).getCubeCache().getLoadedCube(this.field_76635_g, cubeY, this.field_76647_h);
   }

   public Cube getCube(int cubeY) {
      return this.cachedCube != null && this.cachedCube.getY() == cubeY
         ? this.cachedCube
         : ((ICubicWorldInternal)this.getWorld()).getCubeCache().getCube(this.field_76635_g, cubeY, this.field_76647_h);
   }

   @Override
   public void addCube(ICube cube) {
      this.cubeMap.put((Cube)cube);
   }

   public Cube removeCube(int cubeY) {
      if (this.cachedCube != null && this.cachedCube.getY() == cubeY) {
         this.invalidateCachedCube();
      }

      return this.cubeMap.remove(cubeY);
   }

   @Override
   public void removeFromStagingHeightmap(ICube cube) {
      this.stagingHeightMap.removeStagedCube(cube);
   }

   @Override
   public void addToStagingHeightmap(ICube cube) {
      this.stagingHeightMap.addStagedCube(cube);
   }

   @Override
   public int getHeightWithStaging(int localX, int localZ) {
      return !this.isColumn
         ? this.field_76634_f[localZ << 4 | localX]
         : Math.max(this.opacityIndex.getTopBlockY(localX, localZ), this.stagingHeightMap.getTopBlockY(localX, localZ)) + 1;
   }

   private void invalidateCachedCube() {
      this.cachedCube = null;
   }

   @Override
   public boolean hasLoadedCubes() {
      return !this.cubeMap.isEmpty();
   }

   public <T extends World & ICubicWorldInternal> T getWorld() {
      return (T)this.field_76637_e;
   }

   @Override
   public boolean shouldTick() {
      for (Cube cube : this.cubeMap) {
         if (cube.getTickets().shouldTick()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public IHeightMap getOpacityIndex() {
      return this.opacityIndex;
   }

   @Override
   public Collection<? extends ICube> getLoadedCubes() {
      return this.cubeMap.all();
   }

   @Override
   public Iterable<? extends ICube> getLoadedCubes(int startY, int endY) {
      return this.cubeMap.cubes(startY, endY);
   }

   @Override
   public void preCacheCube(ICube cube) {
      this.cachedCube = (Cube)cube;
   }

   @Override
   public int getX() {
      return this.field_76635_g;
   }

   @Override
   public int getZ() {
      return this.field_76647_h;
   }

   @Override
   public int getHeightValue(int localX, int blockY, int localZ) {
      return this.getHeightWithStaging(localX, localZ);
   }

   @Overwrite
   public int func_76611_b(int localX, int localZ) {
      return this.getHeightWithStaging(localX, localZ);
   }

   @Intrinsic
   public int chunk$getHeightValue(int localX, int localZ) {
      return this.getHeightWithStaging(localX, localZ);
   }
}
