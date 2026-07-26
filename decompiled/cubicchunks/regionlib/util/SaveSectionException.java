package cubicchunks.regionlib.util;

import java.io.IOException;
import java.util.Collection;

public class SaveSectionException extends IOException {
   public SaveSectionException(String description, Collection<? extends Throwable> causes) {
      super(description);
      causes.forEach(this::addSuppressed);
   }
}
