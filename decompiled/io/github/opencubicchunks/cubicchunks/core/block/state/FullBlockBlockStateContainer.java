package io.github.opencubicchunks.cubicchunks.core.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraftforge.common.property.IUnlistedProperty;

public class FullBlockBlockStateContainer extends BlockStateContainer {
   public FullBlockBlockStateContainer(Block blockIn, IProperty<?>[] properties) {
      super(blockIn, properties);
   }

   protected StateImplementation createState(
      Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties, @Nullable ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties
   ) {
      return new FullBlockStateImplementation(block, properties);
   }
}
