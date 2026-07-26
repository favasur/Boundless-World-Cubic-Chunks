package cubicchunks.regionlib.lib;

public class RegionEntryLocation {
   private final int offset;
   private final int size;

   public RegionEntryLocation(int offset, int size) {
      this.offset = offset;
      this.size = size;
   }

   public int getOffset() {
      return this.offset;
   }

   public int getSize() {
      return this.size;
   }

   public RegionEntryLocation withSize(int size) {
      return new RegionEntryLocation(this.getOffset(), size);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RegionEntryLocation that = (RegionEntryLocation)o;
         return this.offset != that.offset ? false : this.size == that.size;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.offset;
      return 31 * result + this.size;
   }
}
