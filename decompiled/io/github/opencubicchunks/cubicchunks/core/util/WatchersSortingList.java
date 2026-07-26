package io.github.opencubicchunks.cubicchunks.core.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

public class WatchersSortingList<T> implements Iterable<T> {
   private final Comparator<T> order;
   private Object[] data = new Object[32768];
   private int start = this.data.length / 4;
   private int size = 0;
   private int removed = 0;
   private final Object2IntMap<T> indexMap = new Object2IntOpenHashMap();

   public WatchersSortingList(Comparator<T> orderIn) {
      this.indexMap.defaultReturnValue(-1);
      this.order = (o1, o2) -> {
         boolean o1Removed = o1 == null;
         boolean o2Removed = o2 == null;
         if (o1Removed && o2Removed) {
            return 0;
         } else if (o1Removed) {
            return Integer.MAX_VALUE;
         } else {
            return o2Removed ? Integer.MIN_VALUE : orderIn.compare(o1, o2);
         }
      };
   }

   public void sort() {
      Arrays.sort(this.data, this.start, this.start + this.size, this.order);
      int newSize = Integer.MIN_VALUE;

      for (int i = this.start; i <= this.start + this.size; i++) {
         if (this.data[i] == null) {
            newSize = i - this.start;
            break;
         }

         this.indexMap.put(this.data[i], i);
      }

      assert newSize != Integer.MIN_VALUE;

      this.size = newSize;
      this.removed = 0;
   }

   public boolean isEmpty() {
      return this.size - this.removed == 0;
   }

   @Nonnull
   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         int i = WatchersSortingList.this.start;
         T prev = (T)null;
         T next = (T)null;

         private void peekNext() {
            while (this.next == null && this.i < WatchersSortingList.this.start + WatchersSortingList.this.size) {
               T e = (T)WatchersSortingList.this.data[this.i++];
               if (e != null) {
                  this.next = e;
               }
            }
         }

         @Override
         public boolean hasNext() {
            this.peekNext();
            return this.next != null;
         }

         @Override
         public T next() {
            this.peekNext();
            if (this.next == null) {
               throw new ArrayIndexOutOfBoundsException();
            } else {
               this.prev = this.next;
               this.next = null;
               return this.prev;
            }
         }

         @Override
         public void remove() {
            WatchersSortingList.this.remove(this.prev);
         }
      };
   }

   public void remove(T entry) {
      int idx = this.indexMap.removeInt(entry);
      if (idx >= 0) {
         this.data[idx] = null;
         this.removed++;
      }
   }

   public void removeIf(Predicate<T> predicate) {
      for (int i = this.start; i < this.start + this.size; i++) {
         T a = (T)this.data[i];
         if (a != null && predicate.test(a)) {
            this.indexMap.remove(a);
            this.data[i] = null;
            this.removed++;
         }
      }
   }

   public void appendToStart(T element) {
      if (element == null) {
         throw new NullPointerException("This list does not allow null elements.");
      } else {
         if (this.start <= 0) {
            this.grow();
         }

         this.start--;
         this.data[this.start] = element;
         this.indexMap.put(element, this.start);
         this.size++;
      }
   }

   public void appendToEnd(T element) {
      if (element == null) {
         throw new NullPointerException("This list does not allow null elements.");
      } else {
         if (this.start + this.size >= this.data.length) {
            this.grow();
         }

         this.data[this.start + this.size] = element;
         this.indexMap.put(element, this.start + this.size);
         this.size++;
      }
   }

   public boolean contains(T element) {
      return this.indexMap.containsKey(element);
   }

   private void grow() {
      Object[] newData = new Object[this.data.length * 2];
      int newStart = newData.length / 4;
      System.arraycopy(this.data, this.start, newData, newStart, this.size);
      this.data = newData;
      this.start = newStart;

      for (int i = this.start; i < this.start + this.size; i++) {
         if (this.data[i] != null) {
            this.indexMap.put(this.data[i], i);
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");

      for (Object datum : this.data) {
         sb.append(datum).append(",");
      }

      sb.append("]");
      return sb.toString();
   }
}
