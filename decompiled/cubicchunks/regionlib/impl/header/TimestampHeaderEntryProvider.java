package cubicchunks.regionlib.impl.header;

import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.lib.header.IntHeaderEntry;
import java.util.concurrent.TimeUnit;

public class TimestampHeaderEntryProvider<L extends IKey<L>> implements IHeaderDataEntryProvider<IntHeaderEntry, L> {
   private final TimeUnit timeUnit;

   public TimestampHeaderEntryProvider(TimeUnit timeUnit) {
      this.timeUnit = timeUnit;
   }

   @Override
   public int getEntryByteCount() {
      return 4;
   }

   public IntHeaderEntry apply(L o) {
      return new IntHeaderEntry((int)TimeUnit.MILLISECONDS.convert(System.currentTimeMillis(), this.timeUnit));
   }
}
