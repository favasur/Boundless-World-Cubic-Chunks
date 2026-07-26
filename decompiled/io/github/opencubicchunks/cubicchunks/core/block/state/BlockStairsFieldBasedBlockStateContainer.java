package io.github.opencubicchunks.cubicchunks.core.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraftforge.common.property.IUnlistedProperty;

public class BlockStairsFieldBasedBlockStateContainer extends BlockStateContainer {
   private IBlockState[] propertyValueArray = new IBlockState[127];

   public BlockStairsFieldBasedBlockStateContainer(Block blockIn, IProperty<?>[] properties) {
      super(blockIn, properties);
   }

   protected StateImplementation createState(
      Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties, @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties
   ) {
      if (this.propertyValueArray == null) {
         this.propertyValueArray = new IBlockState[127];
      }

      return new BlockStairsFieldBasedStateImplementation(block, properties, this.propertyValueArray);
   }
}
