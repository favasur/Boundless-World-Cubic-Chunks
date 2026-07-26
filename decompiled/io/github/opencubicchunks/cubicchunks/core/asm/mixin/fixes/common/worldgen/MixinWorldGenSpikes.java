package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.gen.feature.WorldGenSpikes;
import org.spongepowered.asm.mixin.Mixin;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenSpikes.class})
public class MixinWorldGenSpikes {
   public MixinWorldGenSpikes() {
   }
}
