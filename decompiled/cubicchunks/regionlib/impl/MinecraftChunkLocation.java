package cubicchunks.regionlib.impl;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;

public class MinecraftChunkLocation implements IKey<MinecraftChunkLocation> {
   public static final int LOC_BITS = 5;
   public static final int LOC_BITMASK = 31;
   public static final int ENTRIES_PER_REGION = 1024;
   private final int entryX;
   private final int entryZ;
   private String extension;

   public MinecraftChunkLocation(int entryX, int entryZ, String extension) {
      this.entryX = entryX;
      this.entryZ = entryZ;
      this.extension = extension;
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
         MinecraftChunkLocation that = (MinecraftChunkLocation)o;
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
      return new RegionKey("r." + regX + "." + regZ + "." + this.extension);
   }

   @Override
   public int getId() {
      return (this.entryZ & 31) << 5 | this.entryX & 31;
   }

   @Override
   public String toString() {
      return "EntryLocation2D{entryX=" + this.entryX + ", entryZ=" + this.entryZ + ", extension=" + this.extension + "}";
   }

   public static class Provider implements IKeyProvider<MinecraftChunkLocation> {
      private String extension;

      public Provider(String extension) {
         this.extension = extension;
      }

      public MinecraftChunkLocation fromRegionAndId(RegionKey regionKey, int id) throws IllegalArgumentException {
         if (!this.isValid(regionKey)) {
            throw new IllegalArgumentException("Invalid name " + regionKey.getName() + ", expected pattern r\\.-?\\d+\\.-?\\d+\\." + this.extension);
         } else {
            String[] s = regionKey.getName().split("\\.");
            int relativeX = id & 31;
            int relativeZ = id >>> 5;
            return new MinecraftChunkLocation(Integer.parseInt(s[1]) << 5 | relativeX, Integer.parseInt(s[2]) << 5 | relativeZ, this.extension);
         }
      }

      @Override
      public int getKeyCount(RegionKey key) {
         return 1024;
      }

      @Override
      public boolean isValid(RegionKey key) {
         return key.getName().matches("r\\.-?\\d+\\.-?\\d+\\." + this.extension);
      }
   }
}
