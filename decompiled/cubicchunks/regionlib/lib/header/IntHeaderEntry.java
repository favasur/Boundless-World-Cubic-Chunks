package cubicchunks.regionlib.lib.header;

import cubicchunks.regionlib.api.region.header.IHeaderDataEntry;
import java.nio.ByteBuffer;

public class IntHeaderEntry implements IHeaderDataEntry {
   private final int data;

   public IntHeaderEntry(int data) {
      this.data = data;
   }

   @Override
   public void write(ByteBuffer buffer) {
      buffer.putInt(this.data);
   }
}
