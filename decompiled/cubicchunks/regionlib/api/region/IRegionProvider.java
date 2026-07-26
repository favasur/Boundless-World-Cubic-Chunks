package cubicchunks.regionlib.api.region;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedBiConsumer;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Optional;

public interface IRegionProvider<K extends IKey<K>> extends Flushable, Closeable {
   void forRegion(K var1, CheckedConsumer<? super IRegion<K>, IOException> var2) throws IOException;

   <R> Optional<R> fromExistingRegion(K var1, CheckedFunction<? super IRegion<K>, R, IOException> var2) throws IOException;

   <R> R fromRegion(K var1, CheckedFunction<? super IRegion<K>, R, IOException> var2) throws IOException;

   void forExistingRegion(K var1, CheckedConsumer<? super IRegion<K>, IOException> var2) throws IOException;

   IRegion<K> getRegion(K var1) throws IOException;

   Optional<IRegion<K>> getExistingRegion(K var1) throws IOException;

   void forAllRegions(CheckedBiConsumer<RegionKey, ? super IRegion<K>, IOException> var1) throws IOException;
}
