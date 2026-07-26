package io.github.opencubicchunks.cubicchunks.core.asm.mixin;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ICubicWorldSettings {
   boolean isCubic();

   void setCubic(boolean var1);
}
