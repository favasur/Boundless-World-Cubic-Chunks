package cubicchunks.regionlib.lib.header;

import cubicchunks.regionlib.api.region.header.IHeaderDataEntry;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface IKeyIdToSectorMap<H extends IHeaderDataEntry, P extends IHeaderDataEntryProvider<H, K>, K extends IKey<K>>
   extends Iterable<RegionEntryLocation> {
   default Optional<RegionEntryLocation> getEntryLocation(K key) {
      return this.getEntryLocation(key.getId());
   }

   boolean isSpecial(RegionEntryLocation var1);

   Optional<Function<K, ByteBuffer>> trySpecialValue(K var1);

   Optional<RegionEntryLocation> getEntryLocation(int var1);

   Optional<BiConsumer<K, ByteBuffer>> setOffsetAndSize(K var1, RegionEntryLocation var2) throws IOException;

   void setSpecial(K var1, Object var2);

   P headerEntryProvider();
}
