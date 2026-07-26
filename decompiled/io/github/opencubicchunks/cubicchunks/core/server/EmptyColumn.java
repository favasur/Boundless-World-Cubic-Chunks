package io.github.opencubicchunks.cubicchunks.core.server;

import com.google.common.base.Predicate;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;

public class EmptyColumn extends Chunk implements IColumn, IColumnInternal {
   private final ICube emptyCube = new BlankCube(this);

   public EmptyColumn(World worldIn, int x, int z) {
      super(worldIn, x, z);
   }

   public boolean func_76600_a(int x, int z) {
      return x == this.field_76635_g && z == this.field_76647_h;
   }

   public int func_76611_b(int x, int z) {
      return 0;
   }

   @Override
   public int getHeightValue(int localX, int blockY, int localZ) {
      return 0;
   }

   @Override
   public boolean shouldTick() {
      return false;
   }

   @Override
   public IHeightMap getOpacityIndex() {
      return null;
   }

   @Override
   public Collection<? extends ICube> getLoadedCubes() {
      return Collections.emptyList();
   }

   @Override
   public Iterable<? extends ICube> getLoadedCubes(int startY, int endY) {
      return Collections.emptyList();
   }

   @Nullable
   @Override
   public ICube getLoadedCube(int cubeY) {
      return null;
   }

   @Override
   public ICube getCube(int cubeY) {
      return this.emptyCube;
   }

   @Override
   public void addCube(ICube cube) {
      throw new RuntimeException("This should never be called!");
   }

   @Nullable
   @Override
   public ICube removeCube(int cubeY) {
      return null;
   }

   @Override
   public boolean hasLoadedCubes() {
      return false;
   }

   @Override
   public void preCacheCube(ICube cube) {
   }

   public void func_76590_a() {
   }

   public void func_76603_b() {
   }

   public IBlockState func_177435_g(BlockPos pos) {
      return Blocks.field_150350_a.func_176223_P();
   }

   public int func_177437_b(BlockPos pos) {
      return 255;
   }

   public int func_177413_a(EnumSkyBlock type, BlockPos pos) {
      return type.field_77198_c;
   }

   public void func_177431_a(EnumSkyBlock type, BlockPos pos, int value) {
   }

   public int func_177443_a(BlockPos pos, int amount) {
      return 0;
   }

   public void func_76612_a(Entity entityIn) {
   }

   public void func_76622_b(Entity entityIn) {
   }

   public void func_76608_a(Entity entityIn, int index) {
   }

   public boolean func_177444_d(BlockPos pos) {
      return false;
   }

   @Nullable
   public TileEntity func_177424_a(BlockPos pos, EnumCreateEntityType creationMode) {
      return null;
   }

   public void func_150813_a(TileEntity tileEntityIn) {
   }

   public void func_177426_a(BlockPos pos, TileEntity tileEntityIn) {
   }

   public void func_177425_e(BlockPos pos) {
   }

   public void func_76631_c() {
   }

   public void func_76623_d() {
   }

   public void func_76630_e() {
   }

   public void func_177414_a(Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate<? super Entity> filter) {
   }

   public <T extends Entity> void func_177430_a(Class<? extends T> entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate<? super T> filter) {
   }

   public boolean func_76601_a(boolean p_76601_1_) {
      return false;
   }

   public Random func_76617_a(long seed) {
      return new Random(
         this.func_177412_p().func_72905_C()
               + (long)(this.field_76635_g * this.field_76635_g * 4987142)
               + (long)(this.field_76635_g * 5947611)
               + (long)(this.field_76647_h * this.field_76647_h) * 4392871L
               + (long)(this.field_76647_h * 389711)
            ^ seed
      );
   }

   public boolean func_76621_g() {
      return true;
   }

   public boolean func_76606_c(int startY, int endY) {
      return true;
   }

   @Override
   public int getX() {
      return 0;
   }

   @Override
   public int getZ() {
      return 0;
   }

   @Override
   public ChunkPrimer getCompatGenerationPrimer() {
      return null;
   }

   @Override
   public void removeFromStagingHeightmap(ICube cube) {
   }

   @Override
   public void addToStagingHeightmap(ICube cube) {
   }

   @Override
   public int getHeightWithStaging(int localX, int localZ) {
      return 0;
   }
}
