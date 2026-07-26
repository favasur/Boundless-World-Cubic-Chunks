package io.github.opencubicchunks.cubicchunks.core.worldgen.generator;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldGenUtils {
   public WorldGenUtils() {
   }

   public static IBlockState getRandomBedrockReplacement(
      World world, Random rand, IBlockState state, int blockY, int bedrockLevels, boolean topBedrock, boolean bottomBedrock
   ) {
      if (bottomBedrock) {
         int heightAboveBottom = blockY - ((IMinMaxHeight)world).getMinHeight();
         if (heightAboveBottom < bedrockLevels) {
            int bedrockChance = Math.max(1, heightAboveBottom + 1);
            if (rand.nextInt(bedrockChance) == 0) {
               return Blocks.field_150357_h.func_176223_P();
            }
         }
      }

      if (topBedrock) {
         int heightBelowTop = ((IMinMaxHeight)world).getMaxHeight() - blockY - 1;
         if (heightBelowTop < bedrockLevels) {
            int bedrockChance = Math.max(1, heightBelowTop + 1);
            if (rand.nextInt(bedrockChance) == 0) {
               return Blocks.field_150357_h.func_176223_P();
            }
         }
      }

      return state;
   }
}
