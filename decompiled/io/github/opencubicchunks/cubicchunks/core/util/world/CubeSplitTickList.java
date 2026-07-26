package io.github.opencubicchunks.cubicchunks.core.util.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.NextTickListEntry;

public class CubeSplitTickList extends AbstractList<NextTickListEntry> {
   private final Map<CubePos, List<NextTickListEntry>> byCube = new HashMap<>();
   private final List<NextTickListEntry> all = new ArrayList<>();

   public CubeSplitTickList() {
   }

   public List<NextTickListEntry> getForCube(CubePos pos) {
      List<NextTickListEntry> val = this.byCube.get(pos);
      return val == null ? Collections.emptyList() : val;
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
         private final Iterator<NextTickListEntry> it = CubeSplitTickList.this.all.iterator();
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
            CubeSplitTickList.this.removeByCube(this.lastEntry);
         }
      };
   }

   private void removeByCube(NextTickListEntry e) {
      CubePos pos = CubePos.fromBlockCoords(e.field_180282_a);
      List<NextTickListEntry> list = this.byCube.get(pos);
      list.remove(e);
      if (list.isEmpty()) {
         this.byCube.remove(pos);
      }
   }

   private void addByCube(NextTickListEntry e) {
      this.byCube.computeIfAbsent(CubePos.fromBlockCoords(e.field_180282_a), x -> new ArrayList<>()).add(e);
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
      this.all.add(e);
      this.addByCube(e);
      return true;
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
   public void clear() {
      this.all.clear();
      this.byCube.clear();
   }

   public NextTickListEntry get(int index) {
      return this.all.get(index);
   }

   public NextTickListEntry set(int index, NextTickListEntry e) {
      NextTickListEntry old = this.all.set(index, e);
      this.removeByCube(old);
      this.addByCube(e);
      return null;
   }

   public void add(int index, NextTickListEntry element) {
      this.all.add(index, element);
      this.addByCube(element);
   }

   public NextTickListEntry remove(int index) {
      NextTickListEntry old = this.all.remove(index);
      this.removeByCube(old);
      return old;
   }

   @Override
   public int indexOf(Object o) {
      return this.all.indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return this.all.lastIndexOf(o);
   }
}
