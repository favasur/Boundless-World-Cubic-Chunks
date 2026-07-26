package io.github.opencubicchunks.cubicchunks.core.util.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.NextTickListEntry;

public class CubeSplitTickSet implements Set<NextTickListEntry> {
   private final Map<CubePos, CubeSplitTickSet.NextTickListEntryHashSet> byCube = new HashMap<>();
   private final CubeSplitTickSet.NextTickListEntryHashSet all = new CubeSplitTickSet.NextTickListEntryHashSet();

   public CubeSplitTickSet() {
   }

   public Set<NextTickListEntry> getForCube(CubePos pos) {
      Set<NextTickListEntry> val = this.byCube.get(pos);
      return val == null ? Collections.emptySet() : val;
   }

   @Override
   public int size() {
      return this.all.size();
   }

   @Override
   public boolean isEmpty() {
      return this.all.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return this.all.contains(o);
   }

   @Override
   public Iterator<NextTickListEntry> iterator() {
      return new Iterator<NextTickListEntry>() {
         private final Iterator<NextTickListEntry> it = CubeSplitTickSet.this.all.iterator();
         private NextTickListEntry lastEntry = null;

         @Override
         public boolean hasNext() {
            return this.it.hasNext();
         }

         public NextTickListEntry next() {
            return this.lastEntry = this.it.next();
         }

         @Override
         public void remove() {
            this.it.remove();
            CubeSplitTickSet.this.removeByCube(this.lastEntry);
         }
      };
   }

   private void removeByCube(NextTickListEntry e) {
      CubePos pos = CubePos.fromBlockCoords(e.field_180282_a);
      Set<NextTickListEntry> set = this.byCube.get(pos);
      set.remove(e);
      if (set.isEmpty()) {
         this.byCube.remove(pos);
      }
   }

   @Override
   public Object[] toArray() {
      return this.all.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return (T[])this.all.toArray(a);
   }

   public boolean add(NextTickListEntry e) {
      boolean ret = this.all.add(e);
      this.byCube.computeIfAbsent(CubePos.fromBlockCoords(e.field_180282_a), x -> new CubeSplitTickSet.NextTickListEntryHashSet()).add(e);
      return ret;
   }

   @Override
   public boolean remove(Object o) {
      boolean ret = this.all.remove(o);
      if (ret) {
         this.removeByCube((NextTickListEntry)o);
      }

      return ret;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return this.all.containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends NextTickListEntry> c) {
      boolean ret = false;

      for (NextTickListEntry entry : c) {
         if (this.add(entry)) {
            ret = true;
         }
      }

      return ret;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      Iterator<NextTickListEntry> it = this.iterator();
      boolean changed = false;

      while (it.hasNext()) {
         if (!c.contains(it.next())) {
            it.remove();
            changed = true;
         }
      }

      return changed;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean ret = false;

      for (Object entry : c) {
         if (this.remove(entry)) {
            ret = true;
         }
      }

      return ret;
   }

   @Override
   public void clear() {
      this.all.clear();
      this.byCube.clear();
   }

   public static final class EqualsHashCodeWrapper<T extends Comparable<T>> implements Comparable<CubeSplitTickSet.EqualsHashCodeWrapper<T>> {
      final T entry;

      public EqualsHashCodeWrapper(T entry) {
         this.entry = entry;
      }

      @Override
      public int hashCode() {
         return this.entry.hashCode();
      }

      @Override
      public boolean equals(Object entry) {
         return !(entry instanceof CubeSplitTickSet.EqualsHashCodeWrapper) ? false : this.entry.equals(((CubeSplitTickSet.EqualsHashCodeWrapper)entry).entry);
      }

      public int compareTo(CubeSplitTickSet.EqualsHashCodeWrapper<T> other) {
         return this.equals(other) ? 0 : this.entry.compareTo(other.entry);
      }
   }

   public static final class NextTickListEntryHashSet extends AbstractSet<NextTickListEntry> {
      private final Set<CubeSplitTickSet.EqualsHashCodeWrapper<NextTickListEntry>> backingSet = new HashSet<>();

      public NextTickListEntryHashSet() {
      }

      @Override
      public Iterator<NextTickListEntry> iterator() {
         return new Iterator<NextTickListEntry>() {
            final Iterator<CubeSplitTickSet.EqualsHashCodeWrapper<NextTickListEntry>> it = NextTickListEntryHashSet.this.backingSet.iterator();

            @Override
            public boolean hasNext() {
               return this.it.hasNext();
            }

            public NextTickListEntry next() {
               return (NextTickListEntry)this.it.next().entry;
            }
         };
      }

      @Override
      public int size() {
         return this.backingSet.size();
      }

      @Override
      public boolean contains(Object entry) {
         return !(entry instanceof NextTickListEntry) ? false : this.backingSet.contains(new CubeSplitTickSet.EqualsHashCodeWrapper((NextTickListEntry)entry));
      }

      public boolean add(NextTickListEntry entry) {
         return this.backingSet.add(new CubeSplitTickSet.EqualsHashCodeWrapper(entry));
      }

      @Override
      public boolean remove(Object entry) {
         return !(entry instanceof NextTickListEntry) ? false : this.backingSet.remove(new CubeSplitTickSet.EqualsHashCodeWrapper((NextTickListEntry)entry));
      }
   }
}
