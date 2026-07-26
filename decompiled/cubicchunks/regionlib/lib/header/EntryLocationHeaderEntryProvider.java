package cubicchunks.regionlib.lib.header;

import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import java.util.function.ToIntFunction;

public class EntryLocationHeaderEntryProvider<K extends IKey<K>> implements IHeaderDataEntryProvider<IntHeaderEntry, K> {
   private IKeyIdToSectorMap<IntHeaderEntry, EntryLocationHeaderEntryProvider<K>, K> sectorMap;
   private ToIntFunction<RegionEntryLocation> pack;

   public EntryLocationHeaderEntryProvider(
      IKeyIdToSectorMap<IntHeaderEntry, EntryLocationHeaderEntryProvider<K>, K> sectorMap, ToIntFunction<RegionEntryLocation> pack
   ) {
      this.sectorMap = sectorMap;
      this.pack = pack;
   }

   @Override
   public int getEntryByteCount() {
      return 4;
   }

   public IntHeaderEntry apply(K key) {
      return new IntHeaderEntry(this.sectorMap.getEntryLocation(key).map(l -> this.pack.applyAsInt(l)).orElse(0));
   }
}
