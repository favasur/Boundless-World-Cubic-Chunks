package io.github.opencubicchunks.cubicchunks.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class XYZMap<T extends XYZAddressable> implements Iterable<T> {
   private static final Logger LOGGER = LogManager.getLogger("cubicchunks");
   private static final boolean CHECK_THREADED_WRITES = "true".equalsIgnoreCase(System.getProperty("cubicchunks.debug.checkThreadedXYZMapWrites"));
   private static final int HASH_SEED = 1183822147;
   @Nonnull
   private XYZAddressable[] bucketsByPointer;
   @Nonnull
   private XYZAddressable[] bucketsByHash;
   @Nonnull
   private int[] pointers;
   private int size = 0;
   private float loadFactor;
   private int loadThreshold;
   private int mask;
   private final Thread debugStartThreadRef = Thread.currentThread();

   public XYZMap(float loadFactor, int capacity) {
      if ((double)loadFactor > 1.0) {
         throw new IllegalArgumentException("You really dont want to be using a " + loadFactor + " load loadFactor with this hash table!");
      } else {
         this.loadFactor = loadFactor;
         int tCapacity = 1;

         while (tCapacity < capacity) {
            tCapacity <<= 1;
         }

         this.bucketsByPointer = new XYZAddressable[tCapacity];
         this.bucketsByHash = new XYZAddressable[tCapacity];
         this.pointers = new int[tCapacity];
         this.refreshFields();
      }
   }

   public int getSize() {
      return this.size;
   }

   private static int hash(int x, int y, int z) {
      int hash = 1183822147;
      hash += x;
      hash *= 1183822147;
      hash += y;
      hash *= 1183822147;
      hash += z;
      return hash * 1183822147;
   }

   private int getPointerIndex(int x, int y, int z) {
      return hash(x, y, z) & this.mask;
   }

   private int getNextPointerIndex(int pointerIndex) {
      return ++pointerIndex & this.mask;
   }

   public void clear() {
      this.checkThreadedWrite();
      Arrays.fill(this.bucketsByHash, null);
      Arrays.fill(this.bucketsByPointer, null);
      Arrays.fill(this.pointers, 0);
      this.size = 0;
   }

   @Nullable
   public T put(T value) {
      this.checkThreadedWrite();
      int x = value.getX();
      int y = value.getY();
      int z = value.getZ();
      int pointerIndex = this.getPointerIndex(x, y, z);

      for (int index = this.pointers[pointerIndex]; index != 0; index = this.pointers[pointerIndex]) {
         XYZAddressable bucket = this.bucketsByPointer[index];
         if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
            this.bucketsByPointer[index] = value;
            this.bucketsByHash[pointerIndex] = value;
            return (T)bucket;
         }

         pointerIndex = this.getNextPointerIndex(pointerIndex);
      }

      this.bucketsByPointer[++this.size] = value;
      this.bucketsByHash[pointerIndex] = value;
      this.pointers[pointerIndex] = this.size;
      if (this.size > this.loadThreshold) {
         this.grow();
      }

      return null;
   }

   @Nullable
   public T remove(int x, int y, int z) {
      this.checkThreadedWrite();
      int pointerIndex = this.getPointerIndex(x, y, z);

      for (int index = this.pointers[pointerIndex]; index != 0; index = this.pointers[pointerIndex]) {
         XYZAddressable bucket = this.bucketsByPointer[index];
         if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
            this.collapseBucket(pointerIndex, index);
            return (T)bucket;
         }

         pointerIndex = this.getNextPointerIndex(pointerIndex);
      }

      return null;
   }

   @Nullable
   public T remove(T value) {
      return this.remove(value.getX(), value.getY(), value.getZ());
   }

   @Nullable
   public T get(int x, int y, int z) {
      int index = this.getPointerIndex(x, y, z);

      for (XYZAddressable bucket = this.bucketsByHash[index]; bucket != null; bucket = this.bucketsByHash[index]) {
         if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
            return (T)bucket;
         }

         index = this.getNextPointerIndex(index);
      }

      return null;
   }

   public boolean contains(int x, int y, int z) {
      int index = this.getPointerIndex(x, y, z);

      for (XYZAddressable bucket = this.bucketsByHash[index]; bucket != null; bucket = this.bucketsByHash[index]) {
         if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
            return true;
         }

         index = this.getNextPointerIndex(index);
      }

      return false;
   }

   public boolean contains(T value) {
      return this.contains(value.getX(), value.getY(), value.getZ());
   }

   private void grow() {
      int newLength = this.bucketsByPointer.length * 2;
      int newMask = newLength - 1;
      XYZAddressable[] newBucketsByPointer = new XYZAddressable[newLength];
      XYZAddressable[] newBucketsByHash = new XYZAddressable[newLength];
      int[] newPointers = new int[newLength];

      for (int i = 1; i <= this.size; i++) {
         XYZAddressable bucket = this.bucketsByPointer[i];
         newBucketsByPointer[i] = bucket;
         int pointerIndex = hash(bucket.getX(), bucket.getY(), bucket.getZ()) & newMask;

         while (newPointers[pointerIndex] != 0) {
            pointerIndex = ++pointerIndex & newMask;
         }

         newPointers[pointerIndex] = i;
         newBucketsByHash[pointerIndex] = bucket;
      }

      this.bucketsByPointer = newBucketsByPointer;
      this.bucketsByHash = newBucketsByHash;
      this.pointers = newPointers;
      this.mask = newMask;
      this.loadThreshold = (int)((float)newLength * this.loadFactor) - 2;
   }

   private void collapseBucket(int holePointerIndex, int holeIndex) {
      int lastElement = this.size;
      int oldLastPointerIndex = this.getElementPointerIndex(lastElement);
      List<XYZAddressable> nextPointersBuckets = new ArrayList<>(10);
      List<Integer> nextBucketIndexes = new ArrayList<>(10);
      this.pointers[oldLastPointerIndex] = holeIndex;
      this.pointers[holePointerIndex] = 0;
      this.bucketsByPointer[holeIndex] = this.bucketsByPointer[lastElement];
      this.bucketsByPointer[lastElement] = null;
      this.bucketsByHash[holePointerIndex] = null;
      this.size--;
      int pointerIndex = this.getNextPointerIndex(holePointerIndex);

      for (int index = this.pointers[pointerIndex]; index != 0; index = this.pointers[pointerIndex]) {
         XYZAddressable bucket = this.bucketsByPointer[index];
         nextPointersBuckets.add(bucket);
         nextBucketIndexes.add(index);
         this.pointers[pointerIndex] = 0;
         this.bucketsByHash[pointerIndex] = null;
         pointerIndex = this.getNextPointerIndex(pointerIndex);
      }

      for (int i = 0; i < nextPointersBuckets.size(); i++) {
         XYZAddressable bucket = nextPointersBuckets.get(i);
         int x = bucket.getX();
         int y = bucket.getY();
         int z = bucket.getZ();
         int newBucketPointerIndex = this.getPointerIndex(x, y, z);

         for (int newIndex = this.pointers[newBucketPointerIndex]; newIndex != 0; newIndex = this.pointers[newBucketPointerIndex]) {
            newBucketPointerIndex = this.getNextPointerIndex(newBucketPointerIndex);
         }

         this.pointers[newBucketPointerIndex] = nextBucketIndexes.get(i);
         this.bucketsByHash[newBucketPointerIndex] = bucket;
      }
   }

   private int getElementPointerIndex(int index) {
      XYZAddressable lastElement = this.bucketsByPointer[index];
      int pointerIndex = this.getPointerIndex(lastElement.getX(), lastElement.getY(), lastElement.getZ());

      while (this.pointers[pointerIndex] != index) {
         pointerIndex = this.getNextPointerIndex(pointerIndex);
      }

      return pointerIndex;
   }

   private void refreshFields() {
      this.loadThreshold = (int)((float)this.bucketsByPointer.length * this.loadFactor) - 2;
      this.mask = this.bucketsByPointer.length - 1;
   }

   private void checkThreadedWrite() {
      if (CHECK_THREADED_WRITES && Thread.currentThread() != this.debugStartThreadRef) {
         LOGGER.error("Invalid threaded write access", new RuntimeException("Detected XYZ map write access from unexpected thread!"));
      }
   }

   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         int at = 1;

         @Override
         public boolean hasNext() {
            return this.at <= XYZMap.this.size;
         }

         @Nullable
         public T next() {
            return (T)XYZMap.this.bucketsByPointer[this.at++];
         }

         @Override
         public void remove() {
            XYZMap.this.checkThreadedWrite();
            int pointerIndex = XYZMap.this.getElementPointerIndex(--this.at);
            XYZMap.this.collapseBucket(pointerIndex, this.at);
         }
      };
   }

   public Iterator<T> randomWrappedIterator(final int seed) {
      return new Iterator<T>() {
         boolean start = XYZMap.this.size > 0;
         int startFrom = XYZMap.super.start ? XYZMap.this.getNextPointerIndex(seed) % XYZMap.this.size | 1 : 0;
         int at = XYZMap.super.startFrom;

         @Override
         public boolean hasNext() {
            return this.at != this.startFrom || this.start;
         }

         public T next() {
            this.start = false;
            T toReturn = (T)XYZMap.this.bucketsByPointer[this.at++];
            if (this.at > XYZMap.this.size) {
               this.at = 1;
            }

            return toReturn;
         }

         @Override
         public void remove() {
            XYZMap.this.checkThreadedWrite();
            int pointerIndex = XYZMap.this.getElementPointerIndex(--this.at);
            XYZMap.this.collapseBucket(pointerIndex, this.at);
         }
      };
   }
}
