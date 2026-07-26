package cubicchunks.regionlib.lib.provider;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.lib.Region;
import cubicchunks.regionlib.util.CheckedBiConsumer;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class SimpleRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {
   private final IKeyProvider<K> keyProvider;
   private final Path directory;
   private final SimpleRegionProvider.RegionFactory<K> regionBuilder;
   private final SimpleRegionProvider.RegionExistsPredicate<K> regionExists;

   public SimpleRegionProvider(
      IKeyProvider<K> keyProvider,
      Path directory,
      SimpleRegionProvider.RegionFactory<K> regionBuilder,
      SimpleRegionProvider.RegionExistsPredicate<K> regionExists
   ) {
      this.keyProvider = keyProvider;
      this.directory = directory;
      this.regionBuilder = regionBuilder;
      this.regionExists = regionExists;
   }

   @Override
   public <R> Optional<R> fromExistingRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
      IRegion<K> r = this.getExistingRegion(key).orElse(null);
      if (r != null) {
         R ret = func.apply(r);
         r.close();
         return Optional.of(ret);
      } else {
         return Optional.empty();
      }
   }

   @Override
   public <R> R fromRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
      IRegion<K> r = this.getRegion(key);
      R ret = func.apply(r);
      r.close();
      return ret;
   }

   @Override
   public void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> consumer) throws IOException {
      IRegion<K> r = this.getRegion(key);
      consumer.accept(r);
      r.close();
   }

   @Override
   public void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> consumer) throws IOException {
      IRegion<K> r = this.getExistingRegion(key).orElse(null);
      if (r != null) {
         consumer.accept(r);
         r.close();
      }
   }

   @Override
   public IRegion<K> getRegion(K key) throws IOException {
      return this.regionBuilder.create(this.keyProvider, key.getRegionKey());
   }

   @Override
   public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
      Path regionPath = this.directory.resolve(key.getRegionKey().getName());
      if (!this.regionExists.test(regionPath, key)) {
         return Optional.empty();
      } else {
         IRegion<K> reg = this.regionBuilder.create(this.keyProvider, key.getRegionKey());
         return Optional.of(reg);
      }
   }

   @Override
   public void forAllRegions(CheckedBiConsumer<RegionKey, ? super IRegion<K>, IOException> consumer) throws IOException {
      try (Stream<Path> stream = Files.list(this.directory)) {
         Iterator<RegionKey> it = stream.map(Path::getFileName).map(Path::toString).map(RegionKey::new).filter(this.keyProvider::isValid).iterator();

         while (it.hasNext()) {
            RegionKey key = it.next();
            if (this.keyProvider.isValid(key)) {
               consumer.accept(key, this.regionBuilder.create(this.keyProvider, key));
            }
         }
      }
   }

   @Override
   public void flush() throws IOException {
   }

   @Override
   public void close() {
   }

   public static <K extends IKey<K>> SimpleRegionProvider<K> createDefault(IKeyProvider<K> keyProvider, Path directory, int sectorSize) {
      return new SimpleRegionProvider<>(
         keyProvider,
         directory,
         (keyProv, r) -> new Region.Builder<K>().setDirectory(directory).setRegionKey(r).setKeyProvider(keyProv).setSectorSize(sectorSize).build(),
         (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
      );
   }

   @FunctionalInterface
   public interface RegionExistsPredicate<K extends IKey<K>> {
      boolean test(Path var1, K var2) throws IOException;
   }

   @FunctionalInterface
   public interface RegionFactory<K extends IKey<K>> {
      IRegion<K> create(IKeyProvider<K> var1, RegionKey var2) throws IOException;
   }
}
