package io.github.opencubicchunks.cubicchunks.core.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FullBlockStateImplementation extends StateImplementation {
   public FullBlockStateImplementation(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
      super(blockIn, propertiesIn);
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
         collidingBoxes.add(new AxisAlignedBB((double)x1, (double)y1, (double)z1, (double)x2, (double)y2, (double)z2));
      }
   }
}
