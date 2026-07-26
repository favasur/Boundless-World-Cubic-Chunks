package cubicchunks.regionlib.api.region.header;

import cubicchunks.regionlib.api.region.key.IKey;
import java.util.function.Function;

public interface IHeaderDataEntryProvider<H extends IHeaderDataEntry, K extends IKey<K>> extends Function<K, H> {
   int getEntryByteCount();
}
