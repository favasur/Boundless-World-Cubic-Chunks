package cubicchunks.regionlib.util;

import java.io.IOException;

public class CorruptedDataException extends IOException {
   public CorruptedDataException(String text) {
      super(text);
   }
}
