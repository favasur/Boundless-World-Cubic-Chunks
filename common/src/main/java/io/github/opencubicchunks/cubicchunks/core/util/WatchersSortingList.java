package io.github.opencubicchunks.cubicchunks.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.WatchersSortingList
// 1.21: thin ArrayList-derived list that allows sorting on-demand and immutable iteration.
public class WatchersSortingList<T> implements List<T> {

    private final List<T> backing = new ArrayList<>();

    public WatchersSortingList() {
    }

    public WatchersSortingList(int initialCapacity) {
        // 1.21 removed List#ensureCapacity
    }

    public void sortNow(Comparator<? super T> cmp) {
        backing.sort(cmp);
    }

    @Override public int size() { return backing.size(); }
    @Override public boolean isEmpty() { return backing.isEmpty(); }
    @Override public boolean contains(Object o) { return backing.contains(o); }
    @Override public Iterator<T> iterator() { return backing.iterator(); }
    @Override public Object[] toArray() { return backing.toArray(); }
    @Override public <E> E[] toArray(E[] a) { return backing.toArray(a); }
    @Override public boolean add(T t) { return backing.add(t); }
    @Override public boolean remove(Object o) { return backing.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return backing.containsAll(c); }
    @Override public boolean addAll(Collection<? extends T> c) { return backing.addAll(c); }
    @Override public boolean addAll(int index, Collection<? extends T> c) { return backing.addAll(index, c); }
    @Override public boolean removeAll(Collection<?> c) { return backing.removeAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return backing.retainAll(c); }
    @Override public void clear() { backing.clear(); }
    @Override public T get(int index) { return backing.get(index); }
    @Override public T set(int index, T element) { return backing.set(index, element); }
    @Override public void add(int index, T element) { backing.add(index, element); }
    @Override public T remove(int index) { return backing.remove(index); }
    @Override public int indexOf(Object o) { return backing.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return backing.lastIndexOf(o); }
    @Override public ListIterator<T> listIterator() { return backing.listIterator(); }
    @Override public ListIterator<T> listIterator(int index) { return backing.listIterator(index); }
    @Override public List<T> subList(int fromIndex, int toIndex) { return backing.subList(fromIndex, toIndex); }
}
