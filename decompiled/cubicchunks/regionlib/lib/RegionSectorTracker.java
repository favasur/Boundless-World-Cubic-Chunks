package cubicchunks.regionlib.lib;

import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.lib.header.IKeyIdToSectorMap;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.BitSet;
import java.util.Optional;

public class RegionSectorTracker<K extends IKey<K>> {
   private final BitSet usedSectors;
   private IKeyIdToSectorMap<?, ?, K> sectorMap;

   public RegionSectorTracker(BitSet usedSectors, IKeyIdToSectorMap<?, ?, K> sectorMap) {
      this.usedSectors = usedSectors;
      this.sectorMap = sectorMap;
   }

   public void removeKey(K key) throws IOException {
      Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);
      RegionEntryLocation loc = new RegionEntryLocation(0, 0);
      this.sectorMap.setOffsetAndSize(key, loc);
      this.updateUsedSectorsFor(existing.orElse(null), loc);
   }

   public RegionEntryLocation reserveForKey(K key, int requestedSize) throws IOException {
      Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);
      RegionEntryLocation found = this.findSectorFor(existing.orElse(null), requestedSize);
      this.sectorMap.setOffsetAndSize(key, found);
      this.updateUsedSectorsFor(existing.orElse(null), found);
      return found;
   }

   private RegionEntryLocation findSectorFor(RegionEntryLocation oldSector, int requestedSize) {
      int oldSectorSize = oldSector == null ? 0 : oldSector.getSize();
      int newSectorSize = requestedSize;
      if (requestedSize <= oldSectorSize) {
         return oldSector.withSize(requestedSize);
      } else {
         int oldSectorOffset = oldSector == null ? 0 : oldSector.getOffset();
         boolean isEnough = true;

         for (int i = oldSectorOffset + oldSectorSize; i < oldSectorOffset + newSectorSize; i++) {
            if (!this.isSectorFree(i)) {
               isEnough = false;
               break;
            }
         }

         return isEnough ? oldSector.withSize(requestedSize) : this.findNextFree(newSectorSize);
      }
   }

   private RegionEntryLocation findNextFree(int requestedSize) {
      int next = 0;

      int runSize;
      int nextClear;
      do {
         nextClear = this.usedSectors.nextClearBit(next);
         int nextUsed = this.usedSectors.nextSetBit(nextClear);
         next = nextUsed;
         runSize = nextUsed < 0 ? Integer.MAX_VALUE : nextUsed - nextClear;
      } while (runSize < requestedSize);

      return new RegionEntryLocation(nextClear, requestedSize);
   }

   private void updateUsedSectorsFor(RegionEntryLocation oldSectorLocation, RegionEntryLocation newSectorLocation) {
      if (oldSectorLocation != null) {
         int oldOffset = oldSectorLocation.getOffset();
         int oldSize = oldSectorLocation.getSize();

         for (int i = 0; i < oldSize; i++) {
            this.usedSectors.set(oldOffset + i, false);
         }
      }

      if (newSectorLocation != null) {
         int newOffset = newSectorLocation.getOffset();
         int newSize = newSectorLocation.getSize();

         for (int i = 0; i < newSize; i++) {
            this.usedSectors.set(newOffset + i, true);
         }
      }
   }

   private boolean isSectorFree(int sector) {
      return !this.usedSectors.get(sector);
   }

   public static <L extends IKey<L>> RegionSectorTracker<L> fromFile(
      SeekableByteChannel file, IKeyIdToSectorMap<?, ?, L> sectorMap, int reservedSectors, int sectorSize
   ) throws IOException {
      BitSet usedSectors = new BitSet(Math.max((int)(file.size() / (long)sectorSize), reservedSectors));

      for (int i = 0; i < reservedSectors; i++) {
         usedSectors.set(i, true);
      }

      for (RegionEntryLocation loc : sectorMap) {
         if (!sectorMap.isSpecial(loc)) {
            int offset = loc.getOffset();
            int size = loc.getSize();

            for (int i = 0; i < size; i++) {
               usedSectors.set(offset + i);
            }
         }
      }

      return new RegionSectorTracker<>(usedSectors, sectorMap);
   }
}
