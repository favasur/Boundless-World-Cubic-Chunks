package cubicchunks.regionlib.api.storage;

import cubicchunks.regionlib.MultiUnsupportedDataException;
import cubicchunks.regionlib.UnsupportedDataException;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.SaveSectionException;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class SaveSection<S extends SaveSection<S, K>, K extends IKey<K>> implements Flushable, Closeable {
   private static final ByteBuffer DUMMY_EMPTY = ByteBuffer.allocate(0);
   private final List<IRegionProvider<K>> regionProviders;

   public SaveSection(IRegionProvider<K> regionProvider) {
      this(Arrays.asList(regionProvider));
   }

   public SaveSection(List<IRegionProvider<K>> regionProviders) {
      this.regionProviders = regionProviders;
   }

   public void save(K key, ByteBuffer value) throws IOException {
      ByteBuffer toWrite = value;
      List<UnsupportedDataException> exceptions = new ArrayList<>();

      for (IRegionProvider<K> prov : this.regionProviders) {
         ByteBuffer toWriteFinal = toWrite;
         prov.forRegion(key, r -> {
            try {
               r.writeValue(key, toWriteFinal);
               exceptions.clear();
            } catch (UnsupportedDataException var5) {
               exceptions.add(var5);
               r.writeValue(key, null);
            }
         });
         if (exceptions.isEmpty()) {
            toWrite = null;
         }
      }

      if (!exceptions.isEmpty()) {
         throw new SaveSectionException("No region provider supporting key " + key + " with data size " + value.capacity(), exceptions);
      }
   }

   public void save(Map<K, ByteBuffer> entries) throws IOException {
      Map<K, ByteBuffer> pendingEntries = new HashMap<>(entries);
      Map<K, List<UnsupportedDataException>> exceptions = new HashMap<>();
      Map<RegionKey, List<K>> positionsByRegion = pendingEntries.keySet().stream().collect(Collectors.groupingBy(IKey::getRegionKey, Collectors.toList()));

      for (List<K> positionsIn : positionsByRegion.values()) {
         for (IRegionProvider<K> prov : this.regionProviders) {
            prov.forRegion(positionsIn.get(0), r -> {
               List<K> positions = positionsIn;

               try {
                  Map<K, ByteBuffer> regionEntries = new HashMap<>(positions.size());
                  positions.forEach(k -> {
                     ByteBuffer var10000 = regionEntries.put((K)k, pendingEntries.get(k));
                  });
                  r.writeValues(regionEntries);
               } catch (MultiUnsupportedDataException var8x) {
                  Map<K, UnsupportedDataException> children = var8x.getChildren();
                  positions = positionsIn.stream().filter((children::containsKey).negate()).collect(Collectors.toList());
                  children.forEach((k, e) -> exceptions.computeIfAbsent((K)k, unused -> new ArrayList<>()).add(e));
                  Map<K, ByteBuffer> toNulls = new HashMap<>(positions.size());
                  children.forEach((k, v) -> {
                     ByteBuffer var10000 = toNulls.put((K)k, null);
                  });
                  r.writeValues(toNulls);
               }

               positions.forEach(k -> {
                  exceptions.remove(k);
                  pendingEntries.put((K)k, null);
               });
            });
         }
      }

      if (!exceptions.isEmpty()) {
         throw new SaveSectionException(
            "multiple write errors",
            exceptions.entrySet()
               .stream()
               .map(
                  e -> new SaveSectionException("No region provider supporting key " + e.getKey() + " with data size " + entries.get(e.getKey()), e.getValue())
               )
               .collect(Collectors.toList())
         );
      }
   }

   public Optional<ByteBuffer> load(K key, boolean createRegion) throws IOException {
      for (IRegionProvider<K> prov : this.regionProviders) {
         ByteBuffer buf = createRegion
            ? prov.fromRegion(key, r -> r.readValue(key)).orElse(null)
            : prov.<Optional<ByteBuffer>>fromExistingRegion(key, r -> r.readValue(key)).orElse(Optional.of(DUMMY_EMPTY)).orElse(null);
         if (buf != null) {
            return buf == DUMMY_EMPTY ? Optional.empty() : Optional.of(buf);
         }
      }

      return Optional.empty();
   }

   public void forAllKeys(CheckedConsumer<? super K, IOException> cons) throws IOException {
      for (int i = 0; i < this.regionProviders.size(); i++) {
         IRegionProvider<K> p = this.regionProviders.get(i);
         if (i == 0) {
            p.forAllRegions((key, reg) -> {
               reg.forEachKey(cons);
               reg.close();
            });
         } else {
            int max = i;
            p.forAllRegions(
               (regionKey, reg) -> {
                  reg.forEachKey(
                     key -> {
                        for (int j = 0; j < max; j++) {
                           K superKey = this.regionProviders
                              .get(j)
                              .getExistingRegion((K)key)
                              .flatMap(r -> r.hasValue((K)key) ? Optional.of((K)key) : Optional.empty())
                              .orElse(null);
                           if (superKey != null) {
                              return;
                           }
                        }

                        cons.accept(key);
                     }
                  );
                  reg.close();
               }
            );
         }
      }
   }

   public boolean hasEntry(K key) throws IOException {
      for (IRegionProvider<K> prov : this.regionProviders) {
         if (prov.fromExistingRegion(key, r -> r.hasValue(key)).orElse(false)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void flush() throws IOException {
      for (IRegionProvider<K> prov : this.regionProviders) {
         prov.flush();
      }
   }

   @Override
   public void close() throws IOException {
      for (IRegionProvider<K> prov : this.regionProviders) {
         prov.close();
      }
   }
}
