package cubicchunks.regionlib.lib.provider;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedBiConsumer;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SharedCachedRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {
   private final IRegionProvider<K> sourceProvider;
   private static final ReadWriteLock lock = new ReentrantReadWriteLock();
   private static final Map<SharedCachedRegionProvider.SharedCacheKey<?>, IRegion<?>> regionLocationToRegion = new ConcurrentHashMap<>(512);
   private static final int maxCacheSize = 256;
   private boolean closed;

   public SharedCachedRegionProvider(IRegionProvider<K> sourceProvider) {
      this.sourceProvider = sourceProvider;
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
   public void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.forRegion(key, cons, true);
      }
   }

   @Override
   public void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
      if (this.closed) {
         throw new IllegalStateException("Already closed");
      } else {
         this.forRegion(key, cons, false);
      }
   }

   @Override
   public IRegion<K> getRegion(K key) throws IOException {
      SharedCachedRegionProvider.SharedCacheKey<?> sharedKey = new SharedCachedRegionProvider.SharedCacheKey(key.getRegionKey(), this.sourceProvider);
      Lock writeLock = lock.writeLock();
      writeLock.lock();

      IRegion var5;
      try {
         IRegion<K> r = (IRegion<K>)regionLocationToRegion.get(sharedKey);
         if (r == null) {
            return this.sourceProvider.getRegion(key);
         }

         regionLocationToRegion.remove(sharedKey);
         var5 = r;
      } finally {
         writeLock.unlock();
      }

      return var5;
   }

   @Override
   public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
      SharedCachedRegionProvider.SharedCacheKey<?> sharedKey = new SharedCachedRegionProvider.SharedCacheKey(key.getRegionKey(), this.sourceProvider);
      Lock writeLock = lock.writeLock();
      writeLock.lock();

      Optional var5;
      try {
         IRegion<K> r = (IRegion<K>)regionLocationToRegion.get(sharedKey);
         if (r == null) {
            return this.sourceProvider.getExistingRegion(key);
         }

         regionLocationToRegion.remove(sharedKey);
         var5 = Optional.of(r);
      } finally {
         writeLock.unlock();
      }

      return var5;
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
      synchronized (regionLocationToRegion) {
         if (this.closed) {
            throw new IllegalStateException("Already closed");
         } else {
            flushRegions();
            this.sourceProvider.flush();
         }
      }
   }

   @Override
   public void close() throws IOException {
      synchronized (regionLocationToRegion) {
         if (this.closed) {
            throw new IllegalStateException("Already closed");
         } else {
            clearRegions();
            this.sourceProvider.close();
            this.closed = true;
         }
      }
   }

   private void forRegion(K location, CheckedConsumer<? super IRegion<K>, IOException> cons, boolean canCreate) throws IOException {
      Lock readLock = lock.readLock();
      Lock writeLock = lock.writeLock();
      SharedCachedRegionProvider.SharedCacheKey<?> sharedKey = new SharedCachedRegionProvider.SharedCacheKey(location.getRegionKey(), this.sourceProvider);
      boolean createNew = false;
      readLock.lock();

      try {
         IRegion<K> region;
         try {
            region = (IRegion<K>)regionLocationToRegion.computeIfAbsent(sharedKey, shared -> {
               try {
                  return this.sourceProvider.getExistingRegion(location).orElse(null);
               } catch (IOException var4x) {
                  throw new UncheckedIOException(var4x);
               }
            });
         } catch (UncheckedIOException var19) {
            throw var19.getCause();
         }

         if (region == null && canCreate) {
            createNew = true;
         }

         if (region != null) {
            cons.accept(region);
         }
      } finally {
         readLock.unlock();
      }

      if (createNew) {
         writeLock.lock();

         try {
            if (regionLocationToRegion.size() > 256) {
               clearRegions();
            }

            IRegion<K> var21 = this.sourceProvider.getRegion(location);
            regionLocationToRegion.put(sharedKey, var21);
            cons.accept(var21);
         } finally {
            writeLock.unlock();
         }
      }
   }

   public <R> Optional<R> fromRegion(K location, CheckedFunction<? super IRegion<K>, R, IOException> func, boolean canCreate) throws IOException {
      Lock readLock = lock.readLock();
      Lock writeLock = lock.writeLock();
      SharedCachedRegionProvider.SharedCacheKey<?> sharedKey = new SharedCachedRegionProvider.SharedCacheKey(location.getRegionKey(), this.sourceProvider);
      boolean createNew = false;
      readLock.lock();

      try {
         IRegion<K> region;
         try {
            region = (IRegion<K>)regionLocationToRegion.computeIfAbsent(sharedKey, shared -> {
               try {
                  return this.sourceProvider.getExistingRegion(location).orElse(null);
               } catch (IOException var4x) {
                  throw new UncheckedIOException(var4x);
               }
            });
         } catch (UncheckedIOException var18) {
            throw var18.getCause();
         }

         if (region == null && canCreate) {
            createNew = true;
         }

         if (region != null) {
            return Optional.of(func.apply(region));
         }
      } finally {
         readLock.unlock();
      }

      if (createNew) {
         writeLock.lock();

         Optional e;
         try {
            if (regionLocationToRegion.size() > 256) {
               clearRegions();
            }

            IRegion<K> var21 = this.sourceProvider.getRegion(location);
            regionLocationToRegion.put(sharedKey, var21);
            e = Optional.of(func.apply(var21));
         } finally {
            writeLock.unlock();
         }

         return e;
      } else {
         return Optional.empty();
      }
   }

   public static synchronized void flushRegions() throws IOException {
      lock.writeLock().lock();

      try {
         Iterator<IRegion<?>> it = regionLocationToRegion.values().iterator();

         while (it.hasNext()) {
            it.next().flush();
         }
      } finally {
         lock.writeLock().unlock();
      }
   }

   public static synchronized void clearRegions() throws IOException {
      lock.writeLock().lock();

      try {
         Iterator<IRegion<?>> it = regionLocationToRegion.values().iterator();

         while (it.hasNext()) {
            it.next().close();
         }

         regionLocationToRegion.clear();
      } finally {
         lock.writeLock().unlock();
      }
   }

   private static class SharedCacheKey<K extends IKey<K>> {
      private final RegionKey regionKey;
      private final IRegionProvider<K> regionProvider;

      private SharedCacheKey(RegionKey regionKey, IRegionProvider<K> regionProvider) {
         this.regionKey = regionKey;
         this.regionProvider = regionProvider;
      }

      public RegionKey getRegionKey() {
         return this.regionKey;
      }

      public IRegionProvider<K> getRegionProvider() {
         return this.regionProvider;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (!(o instanceof SharedCachedRegionProvider.SharedCacheKey)) {
            return false;
         } else {
            SharedCachedRegionProvider.SharedCacheKey<?> that = (SharedCachedRegionProvider.SharedCacheKey<?>)o;
            return !this.getRegionKey().equals(that.getRegionKey()) ? false : this.getRegionProvider().equals(that.getRegionProvider());
         }
      }

      @Override
      public int hashCode() {
         int result = this.getRegionKey().hashCode();
         return 31 * result + this.getRegionProvider().hashCode();
      }
   }
}
