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

public class NonCollidingBlockStateImplementation extends StateImplementation {
   public NonCollidingBlockStateImplementation(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
      super(blockIn, propertiesIn);
   }

   public void func_185908_a(
      World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState
   ) {
   }
}
