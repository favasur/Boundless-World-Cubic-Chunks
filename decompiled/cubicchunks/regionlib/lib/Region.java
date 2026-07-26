package cubicchunks.regionlib.lib;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.lib.header.IKeyIdToSectorMap;
import cubicchunks.regionlib.lib.header.IntPackedSectorMap;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CorruptedDataException;
import cubicchunks.regionlib.util.Utils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Region<K extends IKey<K>> implements IRegion<K> {
   private final IKeyIdToSectorMap<?, ?, K> sectorMap;
   private final RegionSectorTracker<K> regionSectorTracker;
   private final FileChannel file;
   private final List<IHeaderDataEntryProvider<?, K>> headerEntryProviders;
   private final int sectorSize;
   private final RegionKey regionKey;
   private final IKeyProvider<K> keyProvider;
   private final int keyCount;

   private Region(
      FileChannel file,
      IntPackedSectorMap<K> sectorMap,
      RegionSectorTracker<K> sectorTracker,
      List<IHeaderDataEntryProvider<?, K>> headerEntryProviders,
      RegionKey regionKey,
      IKeyProvider<K> keyProvider,
      int sectorSize
   ) throws IOException {
      this.regionKey = regionKey;
      this.keyProvider = keyProvider;
      this.keyCount = keyProvider.getKeyCount(regionKey);
      this.file = file;
      this.headerEntryProviders = headerEntryProviders;
      this.sectorSize = sectorSize;
      this.sectorMap = sectorMap;
      this.regionSectorTracker = sectorTracker;
   }

   @Override
   public synchronized void writeValue(K key, ByteBuffer value) throws IOException {
      if (value == null) {
         this.regionSectorTracker.removeKey(key);
         this.updateHeaders(key);
      } else {
         int size = value.remaining();
         int sizeWithSizeInfo = size + 4;
         int numSectors = this.getSectorNumber(sizeWithSizeInfo);
         RegionEntryLocation location = this.regionSectorTracker.reserveForKey(key, numSectors);
         int bytesOffset = location.getOffset() * this.sectorSize;
         Utils.writeFully(this.file.position((long)bytesOffset), ByteBuffer.allocate(4).putInt(0, size));
         Utils.writeFully(this.file, value);
         this.updateHeaders(key);
      }
   }

   @Override
   public void writeSpecial(K key, Object marker) throws IOException {
      this.regionSectorTracker.removeKey(key);
      this.sectorMap.setSpecial(key, marker);
      this.updateHeaders(key);
   }

   private void updateHeaders(K key) throws IOException {
      int id = key.getId();
      int currentHeaderBytes = 0;

      for (IHeaderDataEntryProvider<?, K> prov : this.headerEntryProviders) {
         ByteBuffer buf = ByteBuffer.allocate(prov.getEntryByteCount());
         prov.apply(key).write(buf);
         ((Buffer)buf).flip();
         Utils.writeFully(this.file.position((long)(currentHeaderBytes * this.keyCount + id * prov.getEntryByteCount())), buf);
         currentHeaderBytes += prov.getEntryByteCount();
      }
   }

   @Override
   public synchronized Optional<ByteBuffer> readValue(K key) throws IOException {
      try {
         return this.sectorMap.trySpecialValue(key).map(reader -> Optional.of(reader.apply(key))).orElseGet(() -> this.doReadKey(key));
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   private Optional<ByteBuffer> doReadKey(K key) {
      return this.sectorMap.getEntryLocation(key).flatMap(loc -> {
         try {
            int sectorOffset = loc.getOffset();
            int sectorCount = loc.getSize();
            ByteBuffer buf = ByteBuffer.allocate(4);
            Utils.readFully(this.file.position((long)(sectorOffset * this.sectorSize)), buf);
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
      return this.sectorMap.getEntryLocation(key).isPresent();
   }

   @Override
   public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
      for (int id = 0; id < this.keyCount; id++) {
         int idFinal = id;
         K key = this.sectorMap.getEntryLocation(id).map(loc -> this.keyProvider.fromRegionAndId(this.regionKey, idFinal)).orElse(null);
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
      this.flush();
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

   public static <L extends IKey<L>> Region.Builder<L> builder() {
      return new Region.Builder<>();
   }

   public static class Builder<K extends IKey<K>> {
      private Path directory;
      private int sectorSize = 512;
      private List<IHeaderDataEntryProvider<?, K>> headerEntryProviders = new ArrayList<>();
      private RegionKey regionKey;
      private IKeyProvider<K> keyProvider;
      private List<IntPackedSectorMap.SpecialSectorMapEntry<K>> specialEntries = new ArrayList<>();

      public Builder() {
      }

      public Region.Builder<K> setDirectory(Path path) {
         this.directory = path;
         return this;
      }

      public Region.Builder<K> setRegionKey(RegionKey key) {
         this.regionKey = key;
         return this;
      }

      public Region.Builder<K> setKeyProvider(IKeyProvider<K> keyProvider) {
         this.keyProvider = keyProvider;
         return this;
      }

      public Region.Builder<K> setSectorSize(int sectorSize) {
         this.sectorSize = sectorSize;
         return this;
      }

      public Region.Builder<K> addHeaderEntry(IHeaderDataEntryProvider<?, K> headerEntry) {
         this.headerEntryProviders.add(headerEntry);
         return this;
      }

      public Region.Builder<K> addSpecialSectorMapEntry(
         Object marker, int value, Function<K, ByteBuffer> specialReader, BiConsumer<K, ByteBuffer> writeConflictHandler
      ) {
         this.specialEntries.add(new IntPackedSectorMap.SpecialSectorMapEntry<>(marker, value, specialReader, writeConflictHandler));
         return this;
      }

      public Region<K> build() throws IOException {
         FileChannel file = FileChannel.open(
            this.directory.resolve(this.regionKey.getName()), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
         );
         int entryMapBytes = 4;

         for (IHeaderDataEntryProvider<?, ?> prov : this.headerEntryProviders) {
            entryMapBytes += prov.getEntryByteCount();
         }

         int entryMapSectors = Region.ceilDiv(this.keyProvider.getKeyCount(this.regionKey) * entryMapBytes, this.sectorSize);
         IntPackedSectorMap<K> sectorMap = IntPackedSectorMap.readOrCreate(file, this.keyProvider.getKeyCount(this.regionKey), this.specialEntries);
         RegionSectorTracker<K> regionSectorTracker = RegionSectorTracker.fromFile(file, sectorMap, entryMapSectors, this.sectorSize);
         this.headerEntryProviders.add(0, sectorMap.headerEntryProvider());
         return new Region<>(file, sectorMap, regionSectorTracker, this.headerEntryProviders, this.regionKey, this.keyProvider, this.sectorSize);
      }
   }
}
