package cubicchunks.regionlib.lib.header;

import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import cubicchunks.regionlib.util.Utils;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class IntPackedSectorMap<K extends IKey<K>> implements IKeyIdToSectorMap<IntHeaderEntry, EntryLocationHeaderEntryProvider<K>, K> {
   private static final int SIZE_BITS = 8;
   private static final int OFFSET_BITS = 24;
   private static final int SIZE_MASK = 255;
   private static final int MAX_SIZE = 255;
   private static final int OFFSET_MASK = 16777215;
   private static final int MAX_OFFSET = 16777215;
   private final int[] entrySectorOffsets;
   private final List<IntPackedSectorMap.SpecialSectorMapEntry<K>> specialEntries;

   public IntPackedSectorMap(int[] data) {
      this(data, Collections.emptyList());
   }

   public IntPackedSectorMap(int[] data, List<IntPackedSectorMap.SpecialSectorMapEntry<K>> specialEntries) {
      this.entrySectorOffsets = data;
      this.specialEntries = specialEntries;
   }

   @Override
   public Optional<Function<K, ByteBuffer>> trySpecialValue(K key) {
      if (this.specialEntries.isEmpty()) {
         return Optional.empty();
      } else {
         int packed = this.entrySectorOffsets[key.getId()];

         for (IntPackedSectorMap.SpecialSectorMapEntry<K> entry : this.specialEntries) {
            if (entry.rawValue == packed) {
               return Optional.of(entry.reader);
            }
         }

         return Optional.empty();
      }
   }

   @Override
   public Optional<RegionEntryLocation> getEntryLocation(int id) {
      int packed = this.entrySectorOffsets[id];
      return packed == 0 ? Optional.empty() : Optional.of(new RegionEntryLocation(unpackOffset(packed), unpackSize(packed)));
   }

   @Override
   public Optional<BiConsumer<K, ByteBuffer>> setOffsetAndSize(K key, RegionEntryLocation location) throws IOException {
      if (location.getSize() > 255) {
         throw new UnsupportedDataException("Max supported size 255 but requested " + location.getSize());
      } else if (location.getOffset() > 16777215) {
         throw new UnsupportedDataException("Max supported offset 16777215 but requested " + location.getOffset());
      } else {
         BiConsumer<K, ByteBuffer> colflictHandler = null;
         int packed = packed(location);
         if (!this.specialEntries.isEmpty()) {
            for (IntPackedSectorMap.SpecialSectorMapEntry<K> specialEntry : this.specialEntries) {
               if (specialEntry.rawValue == packed) {
                  colflictHandler = specialEntry.conflictHandler;
               }
            }
         }

         this.entrySectorOffsets[key.getId()] = packed;
         return Optional.ofNullable(colflictHandler);
      }
   }

   @Override
   public boolean isSpecial(RegionEntryLocation loc) {
      if (this.specialEntries.isEmpty()) {
         return false;
      } else {
         int packed = packed(loc);

         for (IntPackedSectorMap.SpecialSectorMapEntry<K> specialEntry : this.specialEntries) {
            if (specialEntry.rawValue == packed) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public void setSpecial(K key, Object marker) {
      if (!this.specialEntries.isEmpty()) {
         for (IntPackedSectorMap.SpecialSectorMapEntry<K> specialEntry : this.specialEntries) {
            if (specialEntry.marker == marker) {
               this.entrySectorOffsets[key.getId()] = specialEntry.rawValue;
               return;
            }
         }
      }

      throw new IllegalArgumentException("Unknown marker object" + marker);
   }

   @Override
   public Iterator<RegionEntryLocation> iterator() {
      int first = 0;

      while (first < this.entrySectorOffsets.length && this.entrySectorOffsets[first] == 0) {
         first++;
      }

      final int firstIdx = first;
      return new Iterator<RegionEntryLocation>() {
         int idx = firstIdx;

         @Override
         public boolean hasNext() {
            return this.idx < IntPackedSectorMap.this.entrySectorOffsets.length;
         }

         public RegionEntryLocation next() {
            int packed = IntPackedSectorMap.this.entrySectorOffsets[this.idx];
            RegionEntryLocation loc = new RegionEntryLocation(IntPackedSectorMap.unpackOffset(packed), IntPackedSectorMap.unpackSize(packed));

            do {
               this.idx++;
            } while (this.idx < IntPackedSectorMap.this.entrySectorOffsets.length && IntPackedSectorMap.this.entrySectorOffsets[this.idx] == 0);

            return loc;
         }
      };
   }

   public EntryLocationHeaderEntryProvider<K> headerEntryProvider() {
      return new EntryLocationHeaderEntryProvider<>(this, IntPackedSectorMap::packed);
   }

   private static int unpackOffset(int sectorLocation) {
      return sectorLocation >>> 8;
   }

   private static int unpackSize(int sectorLocation) {
      return sectorLocation & 0xFF;
   }

   private static int packed(RegionEntryLocation location) {
      if ((location.getSize() & 0xFF) != location.getSize()) {
         throw new IllegalArgumentException("Supported entry size range is 0 to 255, but got " + location.getSize());
      } else if ((location.getOffset() & 16777215) != location.getOffset()) {
         throw new IllegalArgumentException("Supported entry offset range is 0 to 16777215, but got " + location.getOffset());
      } else {
         return location.getSize() | location.getOffset() << 8;
      }
   }

   public static <K extends IKey<K>> IntPackedSectorMap<K> readOrCreate(
      SeekableByteChannel file, int entriesPerRegion, List<IntPackedSectorMap.SpecialSectorMapEntry<K>> specialEntries
   ) throws IOException {
      int entryMappingBytes = 4 * entriesPerRegion;
      if (file.size() < (long)entryMappingBytes) {
         file.position(0L);
         Utils.writeFully(file, ByteBuffer.allocate(entryMappingBytes));
      }

      file.position(0L);
      int[] entrySectorOffsets = new int[entriesPerRegion];
      ByteBuffer buffer = ByteBuffer.allocate(entryMappingBytes);
      Utils.readFully(file, buffer);
      ((Buffer)buffer).flip();
      buffer.asIntBuffer().get(entrySectorOffsets);
      return new IntPackedSectorMap<>(entrySectorOffsets, specialEntries);
   }

   public static <L extends IKey<L>> IntPackedSectorMap<L> readOrCreate(SeekableByteChannel file, int entriesPerRegion) throws IOException {
      return readOrCreate(file, entriesPerRegion, Collections.emptyList());
   }

   public static class SpecialSectorMapEntry<K extends IKey<K>> {
      private final Object marker;
      private final int rawValue;
      private final Function<K, ByteBuffer> reader;
      private final BiConsumer<K, ByteBuffer> conflictHandler;

      public SpecialSectorMapEntry(Object marker, int rawValue, Function<K, ByteBuffer> reader, BiConsumer<K, ByteBuffer> conflictHandler) {
         this.marker = marker;
         this.rawValue = rawValue;
         this.reader = reader;
         this.conflictHandler = conflictHandler;
      }
   }
}
