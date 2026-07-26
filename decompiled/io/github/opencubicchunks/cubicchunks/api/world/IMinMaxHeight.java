package io.github.opencubicchunks.cubicchunks.api.world;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IMinMaxHeight {
   default int getMinHeight() {
      return 0;
   }

   default int getMaxHeight() {
      return 256;
   }
}
