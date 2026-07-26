package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

final class SimpleConfigList extends AbstractConfigValue implements ConfigList, Serializable {
   private static final long serialVersionUID = 2L;
   private final List<AbstractConfigValue> value;
   private final boolean resolved;

   SimpleConfigList(ConfigOrigin origin, List<AbstractConfigValue> value) {
      this(origin, value, ResolveStatus.fromValues(value));
   }

   SimpleConfigList(ConfigOrigin origin, List<AbstractConfigValue> value, ResolveStatus status) {
      super(origin);
      this.value = value;
      this.resolved = status == ResolveStatus.RESOLVED;
      if (status != ResolveStatus.fromValues(value)) {
         throw new ConfigException.BugOrBroken("SimpleConfigList created with wrong resolve status: " + this);
      }
   }

   @Override
   public ConfigValueType valueType() {
      return ConfigValueType.LIST;
   }

   @Override
   public List<Object> unwrapped() {
      List<Object> list = new ArrayList<>();

      for (AbstractConfigValue v : this.value) {
         list.add(v.unwrapped());
      }

      return list;
   }

   @Override
   ResolveStatus resolveStatus() {
      return ResolveStatus.fromBoolean(this.resolved);
   }

   private SimpleConfigList modify(AbstractConfigValue.NoExceptionsModifier modifier, ResolveStatus newResolveStatus) {
      try {
         return this.modifyMayThrow(modifier, newResolveStatus);
      } catch (RuntimeException var4) {
         throw var4;
      } catch (Exception var5) {
         throw new ConfigException.BugOrBroken("unexpected checked exception", var5);
      }
   }

   private SimpleConfigList modifyMayThrow(AbstractConfigValue.Modifier modifier, ResolveStatus newResolveStatus) throws Exception {
      List<AbstractConfigValue> changed = null;
      int i = 0;

      for (AbstractConfigValue v : this.value) {
         AbstractConfigValue modified = modifier.modifyChildMayThrow(null, v);
         if (changed == null && modified != v) {
            changed = new ArrayList<>();

            for (int j = 0; j < i; j++) {
               changed.add(this.value.get(j));
            }
         }

         if (changed != null && modified != null) {
            changed.add(modified);
         }

         i++;
      }

      return changed != null ? new SimpleConfigList(this.origin(), changed, newResolveStatus) : this;
   }

   SimpleConfigList resolveSubstitutions(final ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      if (this.resolved) {
         return this;
      } else if (context.isRestrictedToChild()) {
         return this;
      } else {
         try {
            return this.modifyMayThrow(new AbstractConfigValue.Modifier() {
               @Override
               public AbstractConfigValue modifyChildMayThrow(String key, AbstractConfigValue v) throws AbstractConfigValue.NotPossibleToResolve {
                  return context.resolve(v);
               }
            }, ResolveStatus.RESOLVED);
         } catch (AbstractConfigValue.NotPossibleToResolve var3) {
            throw var3;
         } catch (RuntimeException var4) {
            throw var4;
         } catch (Exception var5) {
            throw new ConfigException.BugOrBroken("unexpected checked exception", var5);
         }
      }
   }

   SimpleConfigList relativized(final Path prefix) {
      return this.modify(new AbstractConfigValue.NoExceptionsModifier() {
         @Override
         public AbstractConfigValue modifyChild(String key, AbstractConfigValue v) {
            return v.relativized(prefix);
         }
      }, this.resolveStatus());
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof SimpleConfigList;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof SimpleConfigList) ? false : this.canEqual(other) && this.value.equals(((SimpleConfigList)other).value);
   }

   @Override
   public int hashCode() {
      return this.value.hashCode();
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      if (this.value.isEmpty()) {
         sb.append("[]");
      } else {
         sb.append("[");
         if (options.getFormatted()) {
            sb.append('\n');
         }

         for (AbstractConfigValue v : this.value) {
            if (options.getOriginComments()) {
               indent(sb, indent + 1, options);
               sb.append("# ");
               sb.append(v.origin().description());
               sb.append("\n");
            }

            if (options.getComments()) {
               for (String comment : v.origin().comments()) {
                  indent(sb, indent + 1, options);
                  sb.append("# ");
                  sb.append(comment);
                  sb.append("\n");
               }
            }

            indent(sb, indent + 1, options);
            v.render(sb, indent + 1, atRoot, options);
            sb.append(",");
            if (options.getFormatted()) {
               sb.append('\n');
            }
         }

         sb.setLength(sb.length() - 1);
         if (options.getFormatted()) {
            sb.setLength(sb.length() - 1);
            sb.append('\n');
            indent(sb, indent, options);
         }

         sb.append("]");
      }
   }

   @Override
   public boolean contains(Object o) {
      return this.value.contains(o);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return this.value.containsAll(c);
   }

   public AbstractConfigValue get(int index) {
      return this.value.get(index);
   }

   @Override
   public int indexOf(Object o) {
      return this.value.indexOf(o);
   }

   @Override
   public boolean isEmpty() {
      return this.value.isEmpty();
   }

   @Override
   public Iterator<ConfigValue> iterator() {
      final Iterator<AbstractConfigValue> i = this.value.iterator();
      return new Iterator<ConfigValue>() {
         @Override
         public boolean hasNext() {
            return i.hasNext();
         }

         public ConfigValue next() {
            return i.next();
         }

         @Override
         public void remove() {
            throw SimpleConfigList.weAreImmutable("iterator().remove");
         }
      };
   }

   @Override
   public int lastIndexOf(Object o) {
      return this.value.lastIndexOf(o);
   }

   private static ListIterator<ConfigValue> wrapListIterator(final ListIterator<AbstractConfigValue> i) {
      return new ListIterator<ConfigValue>() {
         @Override
         public boolean hasNext() {
            return i.hasNext();
         }

         public ConfigValue next() {
            return i.next();
         }

         @Override
         public void remove() {
            throw SimpleConfigList.weAreImmutable("listIterator().remove");
         }

         public void add(ConfigValue arg0) {
            throw SimpleConfigList.weAreImmutable("listIterator().add");
         }

         @Override
         public boolean hasPrevious() {
            return i.hasPrevious();
         }

         @Override
         public int nextIndex() {
            return i.nextIndex();
         }

         public ConfigValue previous() {
            return i.previous();
         }

         @Override
         public int previousIndex() {
            return i.previousIndex();
         }

         public void set(ConfigValue arg0) {
            throw SimpleConfigList.weAreImmutable("listIterator().set");
         }
      };
   }

   @Override
   public ListIterator<ConfigValue> listIterator() {
      return wrapListIterator(this.value.listIterator());
   }

   @Override
   public ListIterator<ConfigValue> listIterator(int index) {
      return wrapListIterator(this.value.listIterator(index));
   }

   @Override
   public int size() {
      return this.value.size();
   }

   @Override
   public List<ConfigValue> subList(int fromIndex, int toIndex) {
      List<ConfigValue> list = new ArrayList<>();

      for (AbstractConfigValue v : this.value.subList(fromIndex, toIndex)) {
         list.add(v);
      }

      return list;
   }

   @Override
   public Object[] toArray() {
      return this.value.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return (T[])this.value.toArray(a);
   }

   private static UnsupportedOperationException weAreImmutable(String method) {
      return new UnsupportedOperationException("ConfigList is immutable, you can't call List.'" + method + "'");
   }

   public boolean add(ConfigValue e) {
      throw weAreImmutable("add");
   }

   public void add(int index, ConfigValue element) {
      throw weAreImmutable("add");
   }

   @Override
   public boolean addAll(Collection<? extends ConfigValue> c) {
      throw weAreImmutable("addAll");
   }

   @Override
   public boolean addAll(int index, Collection<? extends ConfigValue> c) {
      throw weAreImmutable("addAll");
   }

   @Override
   public void clear() {
      throw weAreImmutable("clear");
   }

   @Override
   public boolean remove(Object o) {
      throw weAreImmutable("remove");
   }

   public ConfigValue remove(int index) {
      throw weAreImmutable("remove");
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      throw weAreImmutable("removeAll");
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      throw weAreImmutable("retainAll");
   }

   public ConfigValue set(int index, ConfigValue element) {
      throw weAreImmutable("set");
   }

   protected SimpleConfigList newCopy(ConfigOrigin newOrigin) {
      return new SimpleConfigList(newOrigin, this.value);
   }

   final SimpleConfigList concatenate(SimpleConfigList other) {
      ConfigOrigin combinedOrigin = SimpleConfigOrigin.mergeOrigins(this.origin(), other.origin());
      List<AbstractConfigValue> combined = new ArrayList<>(this.value.size() + other.value.size());
      combined.addAll(this.value);
      combined.addAll(other.value);
      return new SimpleConfigList(combinedOrigin, combined);
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SerializedConfigValue(this);
   }
}
