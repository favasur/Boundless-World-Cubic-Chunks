package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.worldgen;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.gen.feature.WorldGenVines;
import org.spongepowered.asm.mixin.Mixin;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldGenVines.class})
public class MixinWorldGenVines {
   public MixinWorldGenVines() {
   }
}
