package io.github.opencubicchunks.cubicchunks.core.worldgen.generator;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils
public class WorldGenUtils {
    private WorldGenUtils() {
    }

    public static BlockState getRandomBedrockReplacement(
            Level world, Random rand, BlockState state,
            int blockY, int bedrockLevels, boolean topBedrock, boolean bottomBedrock) {
        int minHeight = world.getMinBuildHeight();
        int maxHeight = world.getMaxBuildHeight();
        if (world instanceof IMinMaxHeight custom) {
            minHeight = custom.getMinHeight();
            maxHeight = custom.getMaxHeight();
        }
        if (bottomBedrock) {
            int heightAboveBottom = blockY - minHeight;
            if (heightAboveBottom < bedrockLevels) {
                int bedrockChance = Math.max(1, heightAboveBottom + 1);
                if (rand.nextInt(bedrockChance) == 0) {
                    return Blocks.BEDROCK.defaultBlockState();
                }
            }
        }
        if (topBedrock) {
            int heightBelowTop = maxHeight - blockY - 1;
            if (heightBelowTop < bedrockLevels) {
                int bedrockChance = Math.max(1, heightBelowTop + 1);
                if (rand.nextInt(bedrockChance) == 0) {
                    return Blocks.BEDROCK.defaultBlockState();
                }
            }
        }
        return state;
    }
}
