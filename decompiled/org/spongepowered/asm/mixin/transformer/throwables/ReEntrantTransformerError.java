package org.spongepowered.asm.mixin.transformer.throwables;

public class ReEntrantTransformerError extends MixinTransformerError {
   private static final long serialVersionUID = 7073583236491579255L;

   public ReEntrantTransformerError(String message) {
      super(message);
   }

   public ReEntrantTransformerError(Throwable cause) {
      super(cause);
   }

   public ReEntrantTransformerError(String message, Throwable cause) {
      super(message, cause);
   }
}
