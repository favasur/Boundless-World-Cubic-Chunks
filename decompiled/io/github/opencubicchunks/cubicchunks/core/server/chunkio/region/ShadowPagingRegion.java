package io.github.opencubicchunks.cubicchunks.core.server.chunkio.region;

import cubicchunks.regionlib.MultiUnsupportedDataException;
import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import cubicchunks.regionlib.lib.header.IKeyIdToSectorMap;
import cubicchunks.regionlib.lib.header.IntPackedSectorMap;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CorruptedDataException;
import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.util.Tuple;

public class ShadowPagingRegion<K extends IKey<K>> implements IRegion<K> {
   private final FileChannel file;
   private final IHeaderDataEntryProvider<?, K> headerEntryProvider;
   private final RegionKey regionKey;
   private final IKeyProvider<K> keyProvider;
   private final int sectorSize;
   private final ShadowPagingRegion.SectorTracker<K> sectorTracker;

   private ShadowPagingRegion(
      FileChannel file,
      ShadowPagingRegion.SectorTracker<K> sectorTracker,
      IHeaderDataEntryProvider<?, K> headerEntryProvider,
      RegionKey regionKey,
      IKeyProvider<K> keyProvider,
      int sectorSize
   ) {
      this.file = file;
      this.headerEntryProvider = headerEntryProvider;
      this.regionKey = regionKey;
      this.keyProvider = keyProvider;
      this.sectorSize = sectorSize;
      this.sectorTracker = sectorTracker;
   }

   @Override
   public void writeValue(K key, ByteBuffer value) throws IOException {
      CubicChunks.LOGGER.warn("Using slow non-batch write at {} in {}", key, this.regionKey.getName());
      this.writeValues(Collections.singletonMap(key, value));
   }

   @Override
   public synchronized void writeValues(Map<K, ByteBuffer> entries) throws IOException {
      if (!entries.isEmpty()) {
         List<UnsupportedDataException.WithKey> exceptions = new ArrayList<>();
         Map<K, Optional<RegionEntryLocation>> pendingHeaderUpdates = new HashMap<>(entries.size());
         boolean shouldFlush = false;

         for (Entry<K, ByteBuffer> entry : entries.entrySet()) {
            K key = entry.getKey();
            ByteBuffer value = entry.getValue();

            try {
               Optional<RegionEntryLocation> prevLocation;
               if (value == null) {
                  prevLocation = null;
               } else {
                  int size = value.remaining();
                  int sizeWithSizeInfo = size + 4;
                  int numSectors = this.getSectorNumber(sizeWithSizeInfo);
                  Tuple<RegionEntryLocation, RegionEntryLocation> headerUpdate = this.sectorTracker.reserveForKey(key, numSectors);
                  prevLocation = Optional.ofNullable((RegionEntryLocation)headerUpdate.func_76341_a());
                  int bytesOffset = ((RegionEntryLocation)headerUpdate.func_76340_b()).getOffset() * this.sectorSize;
                  Utils.writeFully(this.file.position((long)bytesOffset), ByteBuffer.allocate(4).putInt(0, size));
                  Utils.writeFully(this.file, value);
                  shouldFlush = true;
               }

               pendingHeaderUpdates.put(key, prevLocation);
            } catch (UnsupportedDataException var15) {
               exceptions.add(new UnsupportedDataException.WithKey(var15, key));
            }
         }

         if (shouldFlush) {
            this.file.force(true);
         }

         if (!pendingHeaderUpdates.isEmpty()) {
            for (Entry<K, Optional<RegionEntryLocation>> entry : pendingHeaderUpdates.entrySet()) {
               K key = entry.getKey();
               Optional<RegionEntryLocation> prevLocation = entry.getValue();
               if (prevLocation == null) {
                  this.sectorTracker.removeKey(key);
               } else {
                  this.sectorTracker.updateUsedSectorsFor(prevLocation.orElse(null), null);
               }

               this.updateHeaders(key);
            }

            this.file.force(true);
         }

         if (!exceptions.isEmpty()) {
            throw new MultiUnsupportedDataException(exceptions);
         }
      }
   }

   @Override
   public void writeSpecial(K key, Object marker) throws IOException {
      this.sectorTracker.setSpecial(key, marker);
      this.updateHeaders(key);
      this.file.force(false);
   }

   private void updateHeaders(K key) throws IOException {
      int entryByteCount = this.headerEntryProvider.getEntryByteCount();
      ByteBuffer buf = ByteBuffer.allocate(entryByteCount);
      this.headerEntryProvider.apply(key).write(buf);
      ((Buffer)buf).flip();
      Utils.writeFully(this.file.position((long)key.getId() * (long)entryByteCount), buf);
   }

   @Override
   public synchronized Optional<ByteBuffer> readValue(K key) throws IOException {
      try {
         return this.sectorTracker.trySpecialValue(key).map(reader -> Optional.of(reader.apply(key))).orElseGet(() -> this.doReadKey(key));
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   private Optional<ByteBuffer> doReadKey(K key) {
      return this.sectorTracker.getEntryLocation(key).flatMap(loc -> {
         try {
            int sectorOffset = loc.getOffset();
            int sectorCount = loc.getSize();
            ByteBuffer buf = ByteBuffer.allocate(4);
            Utils.readFully(this.file.position((long)sectorOffset * (long)this.sectorSize), buf);
            int dataLength = buf.getInt(0);
            if (dataLength > sectorCount * this.sectorSize) {
               throw new CorruptedDataException("Expected data size max" + sectorCount * this.sectorSize + " but found " + dataLength);
            } else {
               ByteBuffer bytes = ByteBuffer.allocate(dataLength);
               Utils.readFully(this.file, bytes);
               ((Buffer)bytes).flip();
               return Optional.of(bytes);
            }
         } catch (IOException var7) {
            throw new UncheckedIOException(var7);
         }
      });
   }

   @Override
   public synchronized boolean hasValue(K key) {
      return this.sectorTracker.getEntryLocation(key).isPresent();
   }

   @Override
   public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
      int keyCount = this.keyProvider.getKeyCount(this.regionKey);

      for (int id = 0; id < keyCount; id++) {
         int idFinal = id;
         K key = this.sectorTracker.getEntryLocation(id).map(loc -> this.keyProvider.fromRegionAndId(this.regionKey, idFinal)).orElse(null);
         if (key != null) {
            cons.accept(key);
         }
      }
   }

   private int getSectorNumber(int bytes) {
      return ceilDiv(bytes, this.sectorSize);
   }

   @Override
   public void flush() throws IOException {
      this.ensureSectorSizeAligned();
      this.file.force(false);
   }

   @Override
   public void close() throws IOException {
      this.ensureSectorSizeAligned();
      this.file.close();
   }

   private void ensureSectorSizeAligned() throws IOException {
      if (this.file.size() % (long)this.sectorSize != 0L) {
         int extra = (int)((long)this.sectorSize - this.file.size() % (long)this.sectorSize);
         ByteBuffer buffer = ByteBuffer.allocateDirect(extra);
         this.file.position(this.file.size());
         Utils.writeFully(this.file, buffer);

         assert this.file.size() % (long)this.sectorSize == 0L;
      }
   }

   private static int ceilDiv(int x, int y) {
      return -Math.floorDiv(-x, y);
   }

   public static <L extends IKey<L>> ShadowPagingRegion.Builder<L> builder() {
      return new ShadowPagingRegion.Builder<>();
   }

   public static class Builder<K extends IKey<K>> {
      private Path directory;
      private int sectorSize = 512;
      private RegionKey regionKey;
      private IKeyProvider<K> keyProvider;

      public Builder() {
      }

      public ShadowPagingRegion.Builder<K> setDirectory(Path path) {
         this.directory = path;
         return this;
      }

      public ShadowPagingRegion.Builder<K> setRegionKey(RegionKey key) {
         this.regionKey = key;
         return this;
      }

      public ShadowPagingRegion.Builder<K> setKeyProvider(IKeyProvider<K> keyProvider) {
         this.keyProvider = keyProvider;
         return this;
      }

      public ShadowPagingRegion.Builder<K> setSectorSize(int sectorSize) {
         this.sectorSize = sectorSize;
         return this;
      }

      public ShadowPagingRegion<K> build() throws IOException {
         FileChannel file = FileChannel.open(
            this.directory.resolve(this.regionKey.getName()), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
         );
         int entryMapBytes = 4;
         int entryMapSectors = ShadowPagingRegion.ceilDiv(this.keyProvider.getKeyCount(this.regionKey) * entryMapBytes, this.sectorSize);
         IntPackedSectorMap<K> sectorMap = IntPackedSectorMap.readOrCreate(file, this.keyProvider.getKeyCount(this.regionKey), new ArrayList<>());
         ShadowPagingRegion.SectorTracker<K> regionSectorTracker = ShadowPagingRegion.SectorTracker.fromFile(file, sectorMap, entryMapSectors, this.sectorSize);
         return new ShadowPagingRegion<>(file, regionSectorTracker, sectorMap.headerEntryProvider(), this.regionKey, this.keyProvider, this.sectorSize);
      }
   }

   private static class SectorTracker<K extends IKey<K>> {
      private final BitSet usedSectors;
      private final IKeyIdToSectorMap<?, ?, K> sectorMap;

      private SectorTracker(BitSet usedSectors, IKeyIdToSectorMap<?, ?, K> sectorMap) {
         this.usedSectors = usedSectors;
         this.sectorMap = sectorMap;
      }

      public Optional<RegionEntryLocation> getEntryLocation(int id) {
         return this.sectorMap.getEntryLocation(id);
      }

      public Optional<RegionEntryLocation> getEntryLocation(K key) {
         return this.sectorMap.getEntryLocation(key);
      }

      public void setSpecial(K key, Object obj) throws IOException {
         this.removeKey(key);
         this.sectorMap.setSpecial(key, obj);
      }

      public Optional<Function<K, ByteBuffer>> trySpecialValue(K key) {
         return this.sectorMap.trySpecialValue(key);
      }

      public void removeKey(K key) throws IOException {
         Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);
         RegionEntryLocation loc = new RegionEntryLocation(0, 0);
         this.sectorMap.setOffsetAndSize(key, loc);
         this.updateUsedSectorsFor(existing.orElse(null), loc);
      }

      public Tuple<RegionEntryLocation, RegionEntryLocation> reserveForKey(K key, int requestedSize) throws IOException {
         Optional<RegionEntryLocation> existing = this.sectorMap.getEntryLocation(key);
         RegionEntryLocation found = this.findFree(requestedSize);
         this.sectorMap.setOffsetAndSize(key, found);
         this.updateUsedSectorsFor(null, found);
         return new Tuple(existing.orElse(null), found);
      }

      private RegionEntryLocation findFree(int requestedSize) {
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
            this.usedSectors.set(oldOffset, oldOffset + oldSectorLocation.getSize(), false);
         }

         if (newSectorLocation != null) {
            int newOffset = newSectorLocation.getOffset();
            this.usedSectors.set(newOffset, newOffset + newSectorLocation.getSize(), true);
         }
      }

      private boolean isSectorFree(int sector) {
         return !this.usedSectors.get(sector);
      }

      public static <L extends IKey<L>> ShadowPagingRegion.SectorTracker<L> fromFile(
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

         return new ShadowPagingRegion.SectorTracker<>(usedSectors, sectorMap);
      }
   }
}
