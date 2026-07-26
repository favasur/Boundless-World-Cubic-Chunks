package cubicchunks.regionlib.lib;

import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntry;
import cubicchunks.regionlib.api.region.header.IHeaderDataEntryProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.Utils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ExtRegion<K extends IKey<K>> implements IRegion<K> {
   private final Path directory;
   private final List<IHeaderDataEntryProvider<?, K>> headerData;
   private final IKeyProvider<K> keyProvider;
   private final RegionKey regionKey;
   private final int totalHeaderSize;
   private final BitSet exists;
   private boolean initialized = false;

   public ExtRegion(Path saveDirectory, List<IHeaderDataEntryProvider<?, K>> headerData, IKeyProvider<K> keyProvider, RegionKey regionKey) throws IOException {
      this.directory = saveDirectory.resolve(regionKey.getName() + ".ext");
      this.headerData = headerData;
      this.keyProvider = keyProvider;
      this.regionKey = regionKey;
      int headerSize = 0;

      for (IHeaderDataEntryProvider<?, ?> p : headerData) {
         headerSize += p.getEntryByteCount();
      }

      this.totalHeaderSize = headerSize;
      this.exists = new BitSet(keyProvider.getKeyCount(regionKey));
      if (Files.exists(this.directory)) {
         this.initialized = true;

         try (Stream<Path> stream = Files.list(this.directory)) {
            stream.forEach(px -> {
               String name = px.getFileName().toString();

               try {
                  int i = Integer.parseInt(name);
                  if (i >= 0 && i < keyProvider.getKeyCount(regionKey)) {
                     this.exists.set(i);
                  }
               } catch (NumberFormatException var6) {
               }
            });
         }
      }
   }

   @Override
   public void writeValue(K key, ByteBuffer value) throws IOException {
      if (value != null || this.initialized && !this.exists.isEmpty() && this.exists.get(key.getId())) {
         if (!this.initialized) {
            Utils.createDirectories(this.directory);
            this.initialized = true;
         }

         String fileName = String.valueOf(key.getId());
         Path file = this.directory.resolve(fileName);
         if (!Files.exists(file)) {
            if (value == null) {
               this.exists.clear(key.getId());
               return;
            }
         } else if (value == null) {
            Files.delete(file);
            this.exists.clear(key.getId());
            return;
         }

         Path tmpFile = this.directory.resolve(fileName + ".tmp");
         List<ByteBuffer> buffers = new ArrayList<>(this.headerData.size() + 1);

         for (IHeaderDataEntryProvider<?, K> h : this.headerData) {
            IHeaderDataEntry entry = h.apply(key);
            ByteBuffer buf = ByteBuffer.allocate(h.getEntryByteCount());
            entry.write(buf);
            ((Buffer)buf).flip();
            buffers.add(buf);
         }

         buffers.add(value);

         try (GatheringByteChannel channel = FileChannel.open(
               tmpFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC
            )) {
            Utils.writeFully(channel, buffers.toArray(new ByteBuffer[0]));
         }

         Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         this.exists.set(key.getId());
      }
   }

   @Override
   public void writeSpecial(K key, Object marker) throws IOException {
      throw new UnsupportedOperationException("ExtRegion doesn't support special values");
   }

   @Override
   public Optional<ByteBuffer> readValue(K key) throws IOException {
      if (this.initialized && this.exists.get(key.getId())) {
         Path file = this.directory.resolve(String.valueOf(key.getId()));
         if (!Files.exists(file)) {
            this.exists.set(key.getId(), false);
            return Optional.empty();
         } else {
            Optional var8;
            try (SeekableByteChannel channel = Files.newByteChannel(file)) {
               long size = channel.size();
               if (size > 2147483647L) {
                  throw new UnsupportedDataException("Size " + size + " is too big");
               }

               ByteBuffer buf = ByteBuffer.wrap(new byte[(int)(size - (long)this.totalHeaderSize)]);
               Utils.readFully(channel.position((long)this.totalHeaderSize), buf);
               ((Buffer)buf).rewind();
               var8 = Optional.of(buf);
            }

            return var8;
         }
      } else {
         return Optional.empty();
      }
   }

   @Override
   public boolean hasValue(K key) {
      return this.exists.get(key.getId());
   }

   @Override
   public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
      try {
         this.exists.stream().mapToObj(i -> this.keyProvider.fromRegionAndId(this.regionKey, i)).forEach(x -> {
            try {
               cons.accept(x);
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         });
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   @Override
   public void flush() throws IOException {
   }

   @Override
   public void close() throws IOException {
   }
}
