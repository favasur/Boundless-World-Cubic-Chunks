package io.github.opencubicchunks.cubicchunks.core.world;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;

public class BlankEntityContainer extends EntityContainer {
   public BlankEntityContainer() {
      this.entities = EntityContainer.EMPTY_ARR[0];
   }

   @Override
   public void addEntity(Entity entity) {
      int i = 0;
   }

   @Override
   public boolean remove(Entity entity) {
      return false;
   }

   @Override
   public void clear() {
   }

   @Override
   public Collection<Entity> getEntities() {
      return Collections.emptyList();
   }

   @Override
   public int size() {
      return 0;
   }

   @Override
   public boolean needsSaving(boolean flag, long time, boolean isModified) {
      return false;
   }

   @Override
   public void markSaved(long time) {
   }

   @Override
   public void writeToNbt(NBTTagCompound nbt, String name, Consumer<Entity> listener) {
   }

   @Override
   public void readFromNbt(NBTTagCompound nbt, String name, World world, Consumer<Entity> listener) {
   }

   public static final class BlankEntityMap extends ClassInheritanceMultiMap<Entity> {
      public BlankEntityMap() {
         super(Entity.class);
      }

      public boolean add(Entity e) {
         new Throwable().printStackTrace();
         return false;
      }

      public boolean remove(Object o) {
         return false;
      }

      public boolean contains(Object o) {
         return false;
      }

      public <S> Iterable<S> func_180215_b(Class<S> cl) {
         return Collections.emptyList();
      }

      public Iterator<Entity> iterator() {
         return Collections.emptyIterator();
      }

      public int size() {
         return 0;
      }
   }
}
