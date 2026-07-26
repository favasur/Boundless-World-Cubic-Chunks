package io.github.opencubicchunks.cubicchunks.api.util;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NotCubicChunksWorldException extends RuntimeException {
   public NotCubicChunksWorldException() {
   }

   public NotCubicChunksWorldException(String message) {
      super(message);
   }

   public NotCubicChunksWorldException(String message, Throwable cause) {
      super(message, cause);
   }

   public NotCubicChunksWorldException(Throwable cause) {
      super(cause);
   }

   protected NotCubicChunksWorldException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}
