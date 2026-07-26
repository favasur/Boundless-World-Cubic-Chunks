package cubicchunks.regionlib;

import java.io.IOException;

public class UnsupportedDataException extends IOException {
   public UnsupportedDataException() {
   }

   public UnsupportedDataException(String message) {
      super(message);
   }

   public UnsupportedDataException(String message, Throwable cause) {
      super(message, cause);
   }

   public UnsupportedDataException(Throwable cause) {
      super(cause);
   }

   public static class WithKey extends UnsupportedDataException {
      private final Object key;

      public WithKey(Object key) {
         this.key = key;
      }

      public WithKey(String message, Object key) {
         super(message);
         this.key = key;
      }

      public WithKey(String message, Throwable cause, Object key) {
         super(message, cause);
         this.key = key;
      }

      public WithKey(Throwable cause, Object key) {
         super(cause);
         this.key = key;
      }

      public <K> K getKey() {
         return (K)this.key;
      }
   }
}
