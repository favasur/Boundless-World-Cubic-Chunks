package io.github.opencubicchunks.cubicchunks.core.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.BlockStairs.EnumShape;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockStairsFieldBasedStateImplementation extends StateImplementation {
   private final IBlockState[] propertyValueArray;
   private final EnumFacing facing;
   private final EnumHalf half;
   private final EnumShape shape;

   public BlockStairsFieldBasedStateImplementation(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn, IBlockState[] propertyValueArrayIn) {
      super(blockIn, propertiesIn);
      this.facing = (EnumFacing)propertiesIn.get(BlockStairs.field_176309_a);
      this.half = (EnumHalf)propertiesIn.get(BlockStairs.field_176308_b);
      this.shape = (EnumShape)propertiesIn.get(BlockStairs.field_176310_M);
      this.propertyValueArray = propertyValueArrayIn;
      this.propertyValueArray[propertyIndex(this.facing, this.half, this.shape)] = this;
   }

   private static int propertyIndex(EnumFacing facingIn, EnumHalf halfIn, EnumShape shapeIn) {
      return facingIn.ordinal() | halfIn.ordinal() << 3 | shapeIn.ordinal() << 4;
   }

   public <T extends Comparable<T>> T func_177229_b(IProperty<T> property) {
      if (property == BlockStairs.field_176309_a) {
         return (T)this.facing;
      } else if (property == BlockStairs.field_176308_b) {
         return (T)this.half;
      } else if (property == BlockStairs.field_176310_M) {
         return (T)this.shape;
      } else {
         throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.func_177230_c().func_176194_O());
      }
   }

   public <T extends Comparable<T>, V extends T> IBlockState func_177226_a(IProperty<T> property, V value) {
      int index = 0;
      if (property == BlockStairs.field_176309_a) {
         index = propertyIndex((EnumFacing)value, this.half, this.shape);
      } else if (property == BlockStairs.field_176308_b) {
         index = propertyIndex(this.facing, (EnumHalf)value, this.shape);
      } else {
         if (property != BlockStairs.field_176310_M) {
            throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.func_177230_c().func_176194_O());
         }

         index = propertyIndex(this.facing, this.half, (EnumShape)value);
      }

      return this.propertyValueArray[index];
   }

   public void func_185908_a(
      World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState
   ) {
      int x1 = pos.func_177958_n();
      int y1 = pos.func_177956_o();
      int z1 = pos.func_177952_p();
      int x2 = x1 + 1;
      int y2 = y1 + 1;
      int z2 = z1 + 1;
      if (entityBox.field_72340_a < (double)x2
         && entityBox.field_72336_d > (double)x1
         && entityBox.field_72338_b < (double)y2
         && entityBox.field_72337_e > (double)y1
         && entityBox.field_72339_c < (double)z2
         && entityBox.field_72334_f > (double)z1) {
         this.func_177230_c().func_185477_a(this, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState);
      }
   }
}
