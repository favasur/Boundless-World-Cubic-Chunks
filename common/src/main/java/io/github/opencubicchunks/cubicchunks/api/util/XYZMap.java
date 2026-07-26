package io.github.opencubicchunks.cubicchunks.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 3D hash map for objects implementing {@link XYZAddressable}.
 * Ported from 1.12.2 Cubic Chunks.
 */
// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.api.util.XYZMap
// @Note randomWrappedIterator was omitted because the decompiled source referenced
// non-existent symbols (XYZMap.super.start/startFrom) and could not be recovered.
public class XYZMap<T extends XYZAddressable> implements Iterable<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("cubicchunks");
    private static final boolean CHECK_THREADED_WRITES = "true".equalsIgnoreCase(System.getProperty("cubicchunks.debug.checkThreadedXYZMapWrites"));
    private static final int HASH_SEED = 1183822147;

    private XYZAddressable[] bucketsByPointer;
    private XYZAddressable[] bucketsByHash;
    private int[] pointers;
    private int size = 0;
    private final float loadFactor;
    private int loadThreshold;
    private int mask;
    private final Thread debugStartThreadRef = Thread.currentThread();

    public XYZMap(float loadFactor, int capacity) {
        if (loadFactor > 1.0) {
            throw new IllegalArgumentException("Load factor " + loadFactor + " is too high for XYZMap");
        }
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

    public int getSize() {
        return this.size;
    }

    private static int hash(int x, int y, int z) {
        int hash = HASH_SEED;
        hash += x;
        hash *= HASH_SEED;
        hash += y;
        hash *= HASH_SEED;
        hash += z;
        return hash * HASH_SEED;
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
                return cast(bucket);
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
                return cast(bucket);
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
                return cast(bucket);
            }
            index = this.getNextPointerIndex(index);
        }
        return null;
    }

    public boolean contains(int x, int y, int z) {
        return this.get(x, y, z) != null;
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
                pointerIndex = (pointerIndex + 1) & newMask;
            }
            newPointers[pointerIndex] = i;
            newBucketsByHash[pointerIndex] = bucket;
        }

        this.bucketsByPointer = newBucketsByPointer;
        this.bucketsByHash = newBucketsByHash;
        this.pointers = newPointers;
        this.mask = newMask;
        this.loadThreshold = (int) ((float) newLength * this.loadFactor) - 2;
    }

    private void collapseBucket(int holePointerIndex, int holeIndex) {
        int lastElement = this.size;
        this.pointers[this.getElementPointerIndex(lastElement)] = holeIndex;
        this.pointers[holePointerIndex] = 0;
        this.bucketsByPointer[holeIndex] = this.bucketsByPointer[lastElement];
        this.bucketsByPointer[lastElement] = null;
        this.bucketsByHash[holePointerIndex] = null;
        this.size--;

        int pointerIndex = this.getNextPointerIndex(holePointerIndex);
        List<XYZAddressable> nextBuckets = new ArrayList<>(10);
        List<Integer> nextIndexes = new ArrayList<>(10);
        for (int index = this.pointers[pointerIndex]; index != 0; index = this.pointers[pointerIndex]) {
            XYZAddressable bucket = this.bucketsByPointer[index];
            nextBuckets.add(bucket);
            nextIndexes.add(index);
            this.pointers[pointerIndex] = 0;
            this.bucketsByHash[pointerIndex] = null;
            pointerIndex = this.getNextPointerIndex(pointerIndex);
        }

        for (int i = 0; i < nextBuckets.size(); i++) {
            XYZAddressable bucket = nextBuckets.get(i);
            int x = bucket.getX();
            int y = bucket.getY();
            int z = bucket.getZ();
            int newBucketPointerIndex = this.getPointerIndex(x, y, z);
            while (this.pointers[newBucketPointerIndex] != 0) {
                newBucketPointerIndex = this.getNextPointerIndex(newBucketPointerIndex);
            }
            this.pointers[newBucketPointerIndex] = nextIndexes.get(i);
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
        this.loadThreshold = (int) ((float) this.bucketsByPointer.length * this.loadFactor) - 2;
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

            @Override
            @Nullable
            public T next() {
                return cast(XYZMap.this.bucketsByPointer[this.at++]);
            }

            @Override
            public void remove() {
                XYZMap.this.checkThreadedWrite();
                int pointerIndex = XYZMap.this.getElementPointerIndex(--this.at);
                XYZMap.this.collapseBucket(pointerIndex, this.at);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private T cast(XYZAddressable bucket) {
        return (T) bucket;
    }
}
