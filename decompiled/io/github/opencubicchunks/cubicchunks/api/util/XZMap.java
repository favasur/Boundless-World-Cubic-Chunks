package io.github.opencubicchunks.cubicchunks.api.util;

import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class XZMap<T extends XZAddressable> implements Iterable<T> {
   private static final int HASH_SEED = 1183822147;
   private XZAddressable[] buckets;
   private int size;
   private float loadFactor;
   private int loadThreshold;
   private int mask;

   public XZMap(float loadFactor, int capacity) {
      if ((double)loadFactor > 1.0) {
         throw new IllegalArgumentException("You really dont want to be using a " + loadFactor + " load loadFactor with this hash table!");
      } else {
         this.loadFactor = loadFactor;
         int tCapacity = 1;

         while (tCapacity < capacity) {
            tCapacity <<= 1;
         }

         this.buckets = new XZAddressable[tCapacity];
         this.refreshFields();
      }
   }

   public int getSize() {
      return this.size;
   }

   private static int hash(int x, int z) {
      int hash = 1183822147;
      hash += x;
      hash *= 1183822147;
      hash += z;
      return hash * 1183822147;
   }

   private int getIndex(int x, int z) {
      return hash(x, z) & this.mask;
   }

   private int getNextIndex(int index) {
      return index + 1 & this.mask;
   }

   public void clear() {
      Arrays.fill(this.buckets, null);
      this.size = 0;
   }

   @Nullable
   public T put(T value) {
      int x = value.getX();
      int z = value.getZ();
      int index = this.getIndex(x, z);

      for (XZAddressable bucket = this.buckets[index]; bucket != null; bucket = this.buckets[index]) {
         if (bucket.getX() == x && bucket.getZ() == z) {
            this.buckets[index] = value;
            return (T)bucket;
         }

         index = this.getNextIndex(index);
      }

      this.buckets[index] = value;
      this.size++;
      if (this.size > this.loadThreshold) {
         this.grow();
      }

      return null;
   }

   @Nullable
   public T remove(int x, int z) {
      int index = this.getIndex(x, z);

      for (XZAddressable bucket = this.buckets[index]; bucket != null; bucket = this.buckets[index]) {
         if (bucket.getX() == x && bucket.getZ() == z) {
            this.collapseBucket(index);
            return (T)bucket;
         }

         index = this.getNextIndex(index);
      }

      return null;
   }

   @Nullable
   public T remove(T value) {
      return this.remove(value.getX(), value.getZ());
   }

   @Nullable
   public T get(int x, int z) {
      int index = this.getIndex(x, z);

      for (XZAddressable bucket = this.buckets[index]; bucket != null; bucket = this.buckets[index]) {
         if (bucket.getX() == x && bucket.getZ() == z) {
            return (T)bucket;
         }

         index = this.getNextIndex(index);
      }

      return null;
   }

   public boolean contains(int x, int z) {
      int index = this.getIndex(x, z);

      for (XZAddressable bucket = this.buckets[index]; bucket != null; bucket = this.buckets[index]) {
         if (bucket.getX() == x && bucket.getZ() == z) {
            return true;
         }

         index = this.getNextIndex(index);
      }

      return false;
   }

   public boolean contains(T value) {
      return this.contains(value.getX(), value.getZ());
   }

   private void grow() {
      XZAddressable[] oldBuckets = this.buckets;
      this.buckets = new XZAddressable[this.buckets.length * 2];
      this.refreshFields();

      for (XZAddressable oldBucket : oldBuckets) {
         if (oldBucket != null) {
            int index = this.getIndex(oldBucket.getX(), oldBucket.getZ());
            XZAddressable bucket = this.buckets[index];

            while (bucket != null) {
               bucket = this.buckets[index = this.getNextIndex(index)];
            }

            this.buckets[index] = oldBucket;
         }
      }
   }

   private void collapseBucket(int hole) {
      assert this.buckets[hole] != null;

      this.size--;
      int currentIndex = hole;

      while (true) {
         currentIndex = this.getNextIndex(currentIndex);
         XZAddressable bucket = this.buckets[currentIndex];
         if (bucket == null) {
            this.buckets[hole] = null;
            return;
         }

         int targetIndex = this.getIndex(bucket.getX(), bucket.getZ());
         if (hole < currentIndex) {
            if (targetIndex <= hole || currentIndex < targetIndex) {
               this.buckets[hole] = bucket;
               hole = currentIndex;
            }
         } else if (hole >= targetIndex && targetIndex > currentIndex) {
            this.buckets[hole] = bucket;
            hole = currentIndex;
         }
      }
   }

   private void refreshFields() {
      this.loadThreshold = Math.min(this.buckets.length - 1, (int)((float)this.buckets.length * this.loadFactor));
      this.mask = this.buckets.length - 1;
   }

   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         int at = -1;
         int next = -1;

         @Override
         public boolean hasNext() {
            if (this.next > this.at) {
               return true;
            } else {
               this.next++;

               while (this.next < XZMap.this.buckets.length) {
                  if (XZMap.this.buckets[this.next] != null) {
                     return true;
                  }

                  this.next++;
               }

               return false;
            }
         }

         @Nullable
         public T next() {
            if (this.next > this.at) {
               this.at = this.next;
               return (T)XZMap.this.buckets[this.at];
            } else {
               this.next++;

               while (this.next < XZMap.this.buckets.length) {
                  if (XZMap.this.buckets[this.next] != null) {
                     this.at = this.next;
                     return (T)XZMap.this.buckets[this.at];
                  }

                  this.next++;
               }

               return null;
            }
         }

         @Override
         public void remove() {
            XZMap.this.collapseBucket(this.at);
            this.next = --this.at;
         }
      };
   }
}
