package cubicchunks.regionlib.lib.provider;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedBiConsumer;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Deprecated
public class CachedRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {
   private final IRegionProvider<K> sourceProvider;
   private final Map<RegionKey, IRegion<K>> regionLocationToRegion;
   private int maxCacheSize;
   private boolean closed;

   public CachedRegionProvider(IRegionProvider<K> sourceProvider, int maxCacheSize) {
      this.sourceProvider = sourceProvider;
      this.regionLocationToRegion = new HashMap<>(maxCacheSize * 2);
      this.maxCacheSize = maxCacheSize;
   }

   @Override
   public <R> Optional<R> fromExistingRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         return this.fromRegion(key, func, false);
      }
   }

   @Override
   public <R> R fromRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         return this.fromRegion(key, func, true).get();
      }
   }

   @Override
   public synchronized void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.forRegion(key, cons, true);
      }
   }

   @Override
   public synchronized void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.forRegion(key, cons, false);
      }
   }

   @Override
   public IRegion<K> getRegion(K key) throws IOException {
      IRegion<K> r = this.regionLocationToRegion.get(key.getRegionKey());
      if (r != null) {
         this.regionLocationToRegion.remove(key.getRegionKey());
         return r;
      } else {
         return this.sourceProvider.getRegion(key);
      }
   }

   @Override
   public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
      IRegion<K> r = this.regionLocationToRegion.get(key.getRegionKey());
      if (r != null) {
         this.regionLocationToRegion.remove(key.getRegionKey());
         return Optional.of(r);
      } else {
         return this.sourceProvider.getExistingRegion(key);
      }
   }

   @Override
   public void forAllRegions(CheckedBiConsumer<RegionKey, ? super IRegion<K>, IOException> consumer) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.sourceProvider.forAllRegions(consumer);
      }
   }

   @Override
   public void flush() throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.flushRegions();
         this.sourceProvider.flush();
      }
   }

   @Override
   public void close() throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.clearRegions();
         this.sourceProvider.close();
         this.closed = true;
      }
   }

   private synchronized void forRegion(K location, CheckedConsumer<? super IRegion<K>, IOException> cons, boolean canCreate) throws IOException {
      if (this.regionLocationToRegion.size() > this.maxCacheSize) {
         this.clearRegions();
      }

      IRegion<K> region = this.regionLocationToRegion.get(location.getRegionKey());
      if (region == null) {
         if (canCreate) {
            region = this.sourceProvider.getRegion(location);
         } else {
            region = this.sourceProvider.getExistingRegion(location).orElse(null);
         }

         if (region != null) {
            this.regionLocationToRegion.put(location.getRegionKey(), region);
            cons.accept(region);
         }
      } else {
         cons.accept(region);
      }
   }

   private synchronized <R> Optional<R> fromRegion(K location, CheckedFunction<? super IRegion<K>, R, IOException> func, boolean canCreate) throws IOException {
      if (this.regionLocationToRegion.size() > this.maxCacheSize) {
         this.clearRegions();
      }

      IRegion<K> region = this.regionLocationToRegion.get(location.getRegionKey());
      if (region == null) {
         if (canCreate) {
            region = this.sourceProvider.getRegion(location);
         } else {
            region = this.sourceProvider.getExistingRegion(location).orElse(null);
         }

         if (region != null) {
            this.regionLocationToRegion.put(location.getRegionKey(), region);
            return Optional.of(func.apply(region));
         }
      }

      return region == null ? Optional.empty() : Optional.of(func.apply(region));
   }

   private void flushRegions() throws IOException {
      Iterator<IRegion<K>> it = this.regionLocationToRegion.values().iterator();

      while (it.hasNext()) {
         it.next().flush();
      }
   }

   private void clearRegions() throws IOException {
      Iterator<IRegion<K>> it = this.regionLocationToRegion.values().iterator();

      while (it.hasNext()) {
         it.next().close();
      }

      this.regionLocationToRegion.clear();
   }
}
