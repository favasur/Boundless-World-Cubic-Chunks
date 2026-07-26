package cubicchunks.regionlib;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiUnsupportedDataException extends IOException {
   public MultiUnsupportedDataException(List<UnsupportedDataException.WithKey> children) {
      children.forEach(this::addSuppressed);
   }

   public <K> Map<K, UnsupportedDataException> getChildren() {
      return Stream.of(this.getSuppressed())
         .map(t -> (UnsupportedDataException.WithKey)t)
         .collect(Collectors.toMap(UnsupportedDataException.WithKey::getKey, e -> (UnsupportedDataException)e));
   }
}
