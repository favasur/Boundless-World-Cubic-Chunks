package cubicchunks.regionlib.impl;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;

public class EntryLocation3D implements IKey<EntryLocation3D> {
   private static final int LOC_BITS = 4;
   private static final int LOC_BITMASK = 15;
   public static final int ENTRIES_PER_REGION = 4096;
   private final int entryX;
   private final int entryY;
   private final int entryZ;

   public EntryLocation3D(int entryX, int entryY, int entryZ) {
      this.entryX = entryX;
      this.entryY = entryY;
      this.entryZ = entryZ;
   }

   public int getEntryX() {
      return this.entryX;
   }

   public int getEntryY() {
      return this.entryY;
   }

   public int getEntryZ() {
      return this.entryZ;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         EntryLocation3D that = (EntryLocation3D)o;
         if (this.entryX != that.entryX) {
            return false;
         } else {
            return this.entryY != that.entryY ? false : this.entryZ == that.entryZ;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.entryX;
      result = 31 * result + this.entryY;
      return 31 * result + this.entryZ;
   }

   @Override
   public RegionKey getRegionKey() {
      int regX = this.entryX >> 4;
      int regY = this.entryY >> 4;
      int regZ = this.entryZ >> 4;
      return new RegionKey(regX + "." + regY + "." + regZ + ".3dr");
   }

   @Override
   public int getId() {
      return (this.entryX & 15) << 8 | (this.entryY & 15) << 4 | this.entryZ & 15;
   }

   @Override
   public String toString() {
      return "EntryLocation3D{entryX=" + this.entryX + ", entryY=" + this.entryY + ", entryZ=" + this.entryZ + '}';
   }

   public static class Provider implements IKeyProvider<EntryLocation3D> {
      public Provider() {
      }

      public EntryLocation3D fromRegionAndId(RegionKey regionKey, int id) throws IllegalArgumentException {
         if (!this.isValid(regionKey)) {
            throw new IllegalArgumentException("Invalid name " + regionKey.getName());
         } else {
            String[] s = regionKey.getName().split("\\.");
            int relativeX = id >>> 8;
            int relativeY = id >>> 4 & 15;
            int relativeZ = id & 15;
            return new EntryLocation3D(
               Integer.parseInt(s[0]) << 4 | relativeX, Integer.parseInt(s[1]) << 4 | relativeY, Integer.parseInt(s[2]) << 4 | relativeZ
            );
         }
      }

      @Override
      public int getKeyCount(RegionKey key) {
         return 4096;
      }

      @Override
      public boolean isValid(RegionKey key) {
         return key.getName().matches("-?\\d+\\.-?\\d+\\.-?\\d+\\.3dr");
      }
   }
}
