package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({IBlockAccess.class})
public interface MixinIBlockAccess_MinMaxHeight extends IMinMaxHeight {
}
