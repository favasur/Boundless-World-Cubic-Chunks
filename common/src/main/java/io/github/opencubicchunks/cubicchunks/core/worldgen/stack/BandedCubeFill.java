package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensions;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.WorldGenUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Per-cube fill routine used when a cube Y falls inside a stacked sub-dim band.
 *
 * <p>The fill is intentionally simple — the MVP path. Each cube in a band gets:
 * <ol>
 *     <li>A deterministic seed derived from {@code (cubeX, cubeY, cubeZ)} so the
 *         contents of the cube are reproducible from the level's world seed.</li>
 *     <li>A 16x16x16 block-state body composed mostly of the band's fill block
 *         (netherrack for the Nether, end_stone for The End).</li>
 *     <li>A bedrock top / bedrock floor overlay driven by the {@link StackedDimension}'s
 *         bedrock flags and the existing {@link WorldGenUtils#getRandomBedrockReplacement}
 *         helper.</li>
 *     <li>Light random carve patterns that hint at cave structure so the band reads as
 *         a place, not a solid block of stone.</li>
 * </ol>
 *
 * <p>Callers MUST not assume the cube has biomes — the per-band biomes surface to
 * listeners via the populate event's palette, not through the cube primer here.</p>
 */
public final class BandedCubeFill {

    private static final BlockState BEDROCK_STATE = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState LAVA_STATE = Blocks.LAVA.defaultBlockState();

    private BandedCubeFill() {
    }

    /**
     * Backward-compatible entry: resolves the per-band fill block from the resource
     * location on the dim record and forwards to {@link #fillBand}.
     */
    public static CubePrimer fillBand(CubePrimer primer, StackedDimension dim,
                                       ServerLevel level, int cubeX, int cubeY, int cubeZ) {
        BlockState fill = tryResolveFillBlock(dim);
        if (fill == null) {
            fill = Blocks.STONE.defaultBlockState();
        }
        return fillBand(primer, dim, level, cubeX, cubeY, cubeZ, fill);
    }

    /**
     * Fill the supplied primer with the per-band content for {@code dim}.
     */
    public static CubePrimer fillBand(CubePrimer primer, StackedDimension dim,
                                       ServerLevel level, int cubeX, int cubeY, int cubeZ,
                                       BlockState fillState) {
        Random rand = deterministicRandom(level, cubeX, cubeY, cubeZ);

        int minBlockY = Coords.cubeToMinBlock(cubeY);
        int maxBlockY = Coords.cubeToMaxBlock(cubeY);
        int dimTop = dim.maxBlockY();
        int dimBottom = dim.minBlockY();

        // 1. Fill the body with the band's fill block.
        for (int y = 0; y < 16; y++) {
            int blockY = minBlockY + y;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (dim.id().equals(StackedDimensions.END_ID)) {
                        // End band: occasional floating pillars of obsidian-ish stone.
                        if (rand.nextInt(96) == 0) {
                            primer.setBlockState(x, y, z, Blocks.OBSIDIAN.defaultBlockState());
                            continue;
                        }
                    } else if (dim.id().equals(StackedDimensions.NETHER_ID)) {
                        // Nether band: lava lakes at the bottom-most cube.
                        if (cubeY == dim.getMinCubeY() && blockY == dimBottom + 1 && rand.nextInt(28) == 0) {
                            primer.setBlockState(x, y, z, LAVA_STATE);
                            continue;
                        }
                        // Glowstone blobs hanging from cube ceilings.
                        if (rand.nextInt(64) == 0 && y > 12) {
                            primer.setBlockState(x, y, z, Blocks.GLOWSTONE.defaultBlockState());
                            continue;
                        }
                    }
                    primer.setBlockState(x, y, z, fillState);
                }
            }
        }

        // 2. Bedrock top / bottom: use the per-band flag + delegated replace helper.
        if (dim.bedrockTop() && maxBlockY >= dimTop - 5 && maxBlockY <= dimTop) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY < dimTop - 4) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                level, rand, fillState, blockY, 5,
                                true /* topBedrock */, false /* bottomBedrock */);
                        primer.setBlockState(x, y, z, replaced);
                    }
                }
            }
        }
        if (dim.bedrockBottom() && minBlockY <= dimBottom + 4 && minBlockY >= dimBottom) {
            for (int y = 0; y < 16; y++) {
                int blockY = minBlockY + y;
                if (blockY > dimBottom + 4) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState replaced = WorldGenUtils.getRandomBedrockReplacement(
                                level, rand, fillState, blockY, 5,
                                false /* topBedrock */, true /* bottomBedrock */);
                        primer.setBlockState(x, y, z, replaced);
                    }
                }
            }
        }
        return primer;
    }

    /**
     * Resolve a block state for the dim's {@code fillBlockId} from the block registry.
     * Returns null if the resource could not be resolved.
     */
    @Nullable
    public static BlockState tryResolveFillBlock(StackedDimension dim) {
        ResourceLocation id = dim.fillBlockId();
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || block == Blocks.AIR) {
            CubicChunks.LOGGER.warn("Could not resolve fill block for sub-dim {}: {}", dim.id(), id);
            return null;
        }
        return block.defaultBlockState();
    }

    /**
     * Deterministic per-(cubeX,cubeY,cubeZ) random source seeded from the level's
     * world seed. Reproducibility means saves with the same world seed produce
     * identical stacked band layouts.
     */
    public static Random deterministicRandom(ServerLevel level, int cubeX, int cubeY, int cubeZ) {
        Random rand = new Random(level.getSeed());
        rand.setSeed(rand.nextInt() ^ cubeX);
        rand.setSeed(rand.nextInt() ^ cubeZ);
        rand.setSeed(rand.nextInt() ^ cubeY);
        return rand;
    }

}
