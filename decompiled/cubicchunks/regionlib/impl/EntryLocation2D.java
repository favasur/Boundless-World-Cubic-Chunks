package cubicchunks.regionlib.impl;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;

public class EntryLocation2D implements IKey<EntryLocation2D> {
   public static final int LOC_BITS = 5;
   public static final int LOC_BITMASK = 31;
   public static final int ENTRIES_PER_REGION = 1024;
   private final int entryX;
   private final int entryZ;

   public EntryLocation2D(int entryX, int entryZ) {
      this.entryX = entryX;
      this.entryZ = entryZ;
   }

   public int getEntryX() {
      return this.entryX;
   }

   public int getEntryZ() {
      return this.entryZ;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         EntryLocation2D that = (EntryLocation2D)o;
         return this.entryX != that.entryX ? false : this.entryZ == that.entryZ;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.entryX;
      return 31 * result + this.entryZ;
   }

   @Override
   public RegionKey getRegionKey() {
      int regX = this.entryX >> 5;
      int regZ = this.entryZ >> 5;
      return new RegionKey(regX + "." + regZ + ".2dr");
   }

   @Override
   public int getId() {
      return (this.entryX & 31) << 5 | this.entryZ & 31;
   }

   @Override
   public String toString() {
      return "EntryLocation2D{entryX=" + this.entryX + ", entryZ=" + this.entryZ + '}';
   }

   public static class Provider implements IKeyProvider<EntryLocation2D> {
      public Provider() {
      }

      public EntryLocation2D fromRegionAndId(RegionKey regionKey, int id) throws IllegalArgumentException {
         if (!this.isValid(regionKey)) {
            throw new IllegalArgumentException("Invalid name " + regionKey.getName());
         } else {
            String[] s = regionKey.getName().split("\\.");
            int relativeX = id >>> 5;
            int relativeZ = id & 31;
            return new EntryLocation2D(Integer.parseInt(s[0]) << 5 | relativeX, Integer.parseInt(s[1]) << 5 | relativeZ);
         }
      }

      @Override
      public int getKeyCount(RegionKey key) {
         return 1024;
      }

      @Override
      public boolean isValid(RegionKey key) {
         return key.getName().matches("-?\\d+\\.-?\\d+\\.2dr");
      }
   }
}
