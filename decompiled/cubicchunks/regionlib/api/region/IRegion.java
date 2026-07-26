package cubicchunks.regionlib.api.region;

import cubicchunks.regionlib.MultiUnsupportedDataException;
import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.util.CheckedConsumer;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

public interface IRegion<K extends IKey<K>> extends Flushable, Closeable {
   void writeValue(K var1, ByteBuffer var2) throws IOException;

   default void writeValues(Map<K, ByteBuffer> entries) throws IOException {
      List<UnsupportedDataException.WithKey> exceptions = new ArrayList<>();

      for (Entry<K, ByteBuffer> entry : entries.entrySet()) {
         try {
            this.writeValue(entry.getKey(), entry.getValue());
         } catch (UnsupportedDataException.WithKey var6) {
            exceptions.add(var6);
         } catch (UnsupportedDataException var7) {
            exceptions.add(new UnsupportedDataException.WithKey(var7, entry.getKey()));
         }
      }

      if (!exceptions.isEmpty()) {
         throw new MultiUnsupportedDataException(exceptions);
      }
   }

   void writeSpecial(K var1, Object var2) throws IOException;

   Optional<ByteBuffer> readValue(K var1) throws IOException;

   boolean hasValue(K var1);

   void forEachKey(CheckedConsumer<? super K, IOException> var1) throws IOException;
}
