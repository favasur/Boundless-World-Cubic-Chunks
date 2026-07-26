package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

final class SimpleConfig implements Config, MergeableValue, Serializable {
   private static final long serialVersionUID = 1L;
   private final AbstractConfigObject object;

   SimpleConfig(AbstractConfigObject object) {
      this.object = object;
   }

   public AbstractConfigObject root() {
      return this.object;
   }

   @Override
   public ConfigOrigin origin() {
      return this.object.origin();
   }

   public SimpleConfig resolve() {
      return this.resolve(ConfigResolveOptions.defaults());
   }

   public SimpleConfig resolve(ConfigResolveOptions options) {
      return this.resolveWith(this, options);
   }

   public SimpleConfig resolveWith(Config source) {
      return this.resolveWith(source, ConfigResolveOptions.defaults());
   }

   public SimpleConfig resolveWith(Config source, ConfigResolveOptions options) {
      AbstractConfigValue resolved = ResolveContext.resolve(this.object, ((SimpleConfig)source).object, options);
      return resolved == this.object ? this : new SimpleConfig((AbstractConfigObject)resolved);
   }

   @Override
   public boolean hasPath(String pathExpression) {
      Path path = Path.newPath(pathExpression);

      ConfigValue peeked;
      try {
         peeked = this.object.peekPath(path);
      } catch (ConfigException.NotResolved var5) {
         throw ConfigImpl.improveNotResolved(path, var5);
      }

      return peeked != null && peeked.valueType() != ConfigValueType.NULL;
   }

   @Override
   public boolean isEmpty() {
      return this.object.isEmpty();
   }

   private static void findPaths(Set<Entry<String, ConfigValue>> entries, Path parent, AbstractConfigObject obj) {
      for (Entry<String, ConfigValue> entry : obj.entrySet()) {
         String elem = entry.getKey();
         ConfigValue v = entry.getValue();
         Path path = Path.newKey(elem);
         if (parent != null) {
            path = path.prepend(parent);
         }

         if (v instanceof AbstractConfigObject) {
            findPaths(entries, path, (AbstractConfigObject)v);
         } else if (!(v instanceof ConfigNull)) {
            entries.add(new SimpleImmutableEntry<>(path.render(), v));
         }
      }
   }

   @Override
   public Set<Entry<String, ConfigValue>> entrySet() {
      Set<Entry<String, ConfigValue>> entries = new HashSet<>();
      findPaths(entries, null, this.object);
      return entries;
   }

   private static AbstractConfigValue findKey(AbstractConfigObject self, String key, ConfigValueType expected, Path originalPath) {
      AbstractConfigValue v = self.peekAssumingResolved(key, originalPath);
      if (v == null) {
         throw new ConfigException.Missing(originalPath.render());
      } else {
         if (expected != null) {
            v = DefaultTransformer.transform(v, expected);
         }

         if (v.valueType() == ConfigValueType.NULL) {
            throw new ConfigException.Null(v.origin(), originalPath.render(), expected != null ? expected.name() : null);
         } else if (expected != null && v.valueType() != expected) {
            throw new ConfigException.WrongType(v.origin(), originalPath.render(), expected.name(), v.valueType().name());
         } else {
            return v;
         }
      }
   }

   private static AbstractConfigValue find(AbstractConfigObject self, Path path, ConfigValueType expected, Path originalPath) {
      try {
         String key = path.first();
         Path next = path.remainder();
         if (next == null) {
            return findKey(self, key, expected, originalPath);
         } else {
            AbstractConfigObject o = (AbstractConfigObject)findKey(
               self, key, ConfigValueType.OBJECT, originalPath.subPath(0, originalPath.length() - next.length())
            );

            assert o != null;

            return find(o, next, expected, originalPath);
         }
      } catch (ConfigException.NotResolved var7) {
         throw ConfigImpl.improveNotResolved(path, var7);
      }
   }

   AbstractConfigValue find(Path pathExpression, ConfigValueType expected, Path originalPath) {
      return find(this.object, pathExpression, expected, originalPath);
   }

   AbstractConfigValue find(String pathExpression, ConfigValueType expected) {
      Path path = Path.newPath(pathExpression);
      return this.find(path, expected, path);
   }

   public AbstractConfigValue getValue(String path) {
      return this.find(path, null);
   }

   @Override
   public boolean getBoolean(String path) {
      ConfigValue v = this.find(path, ConfigValueType.BOOLEAN);
      return (Boolean)v.unwrapped();
   }

   private ConfigNumber getConfigNumber(String path) {
      ConfigValue v = this.find(path, ConfigValueType.NUMBER);
      return (ConfigNumber)v;
   }

   @Override
   public Number getNumber(String path) {
      return this.getConfigNumber(path).unwrapped();
   }

   @Override
   public int getInt(String path) {
      ConfigNumber n = this.getConfigNumber(path);
      return n.intValueRangeChecked(path);
   }

   @Override
   public long getLong(String path) {
      return this.getNumber(path).longValue();
   }

   @Override
   public double getDouble(String path) {
      return this.getNumber(path).doubleValue();
   }

   @Override
   public String getString(String path) {
      ConfigValue v = this.find(path, ConfigValueType.STRING);
      return (String)v.unwrapped();
   }

   @Override
   public ConfigList getList(String path) {
      AbstractConfigValue v = this.find(path, ConfigValueType.LIST);
      return (ConfigList)v;
   }

   public AbstractConfigObject getObject(String path) {
      return (AbstractConfigObject)this.find(path, ConfigValueType.OBJECT);
   }

   public SimpleConfig getConfig(String path) {
      return this.getObject(path).toConfig();
   }

   @Override
   public Object getAnyRef(String path) {
      ConfigValue v = this.find(path, null);
      return v.unwrapped();
   }

   @Override
   public Long getBytes(String path) {
      Long size = null;

      try {
         size = this.getLong(path);
      } catch (ConfigException.WrongType var5) {
         ConfigValue v = this.find(path, ConfigValueType.STRING);
         size = parseBytes((String)v.unwrapped(), v.origin(), path);
      }

      return size;
   }

   @Deprecated
   @Override
   public Long getMilliseconds(String path) {
      return this.getDuration(path, TimeUnit.MILLISECONDS);
   }

   @Deprecated
   @Override
   public Long getNanoseconds(String path) {
      return this.getDuration(path, TimeUnit.NANOSECONDS);
   }

   @Override
   public long getDuration(String path, TimeUnit unit) {
      ConfigValue v = this.find(path, ConfigValueType.STRING);
      return unit.convert(parseDuration((String)v.unwrapped(), v.origin(), path), TimeUnit.NANOSECONDS);
   }

   private <T> List<T> getHomogeneousUnwrappedList(String path, ConfigValueType expected) {
      List<T> l = new ArrayList<>();

      for (ConfigValue cv : this.getList(path)) {
         AbstractConfigValue v = (AbstractConfigValue)cv;
         if (expected != null) {
            v = DefaultTransformer.transform(v, expected);
         }

         if (v.valueType() != expected) {
            throw new ConfigException.WrongType(v.origin(), path, "list of " + expected.name(), "list of " + v.valueType().name());
         }

         l.add((T)v.unwrapped());
      }

      return l;
   }

   @Override
   public List<Boolean> getBooleanList(String path) {
      return this.getHomogeneousUnwrappedList(path, ConfigValueType.BOOLEAN);
   }

   @Override
   public List<Number> getNumberList(String path) {
      return this.getHomogeneousUnwrappedList(path, ConfigValueType.NUMBER);
   }

   @Override
   public List<Integer> getIntList(String path) {
      List<Integer> l = new ArrayList<>();

      for (AbstractConfigValue v : this.getHomogeneousWrappedList(path, ConfigValueType.NUMBER)) {
         l.add(((ConfigNumber)v).intValueRangeChecked(path));
      }

      return l;
   }

   @Override
   public List<Long> getLongList(String path) {
      List<Long> l = new ArrayList<>();

      for (Number n : this.getNumberList(path)) {
         l.add(n.longValue());
      }

      return l;
   }

   @Override
   public List<Double> getDoubleList(String path) {
      List<Double> l = new ArrayList<>();

      for (Number n : this.getNumberList(path)) {
         l.add(n.doubleValue());
      }

      return l;
   }

   @Override
   public List<String> getStringList(String path) {
      return this.getHomogeneousUnwrappedList(path, ConfigValueType.STRING);
   }

   private <T extends ConfigValue> List<T> getHomogeneousWrappedList(String path, ConfigValueType expected) {
      List<T> l = new ArrayList<>();

      for (ConfigValue cv : this.getList(path)) {
         AbstractConfigValue v = (AbstractConfigValue)cv;
         if (expected != null) {
            v = DefaultTransformer.transform(v, expected);
         }

         if (v.valueType() != expected) {
            throw new ConfigException.WrongType(v.origin(), path, "list of " + expected.name(), "list of " + v.valueType().name());
         }

         l.add((T)v);
      }

      return l;
   }

   @Override
   public List<ConfigObject> getObjectList(String path) {
      return this.getHomogeneousWrappedList(path, ConfigValueType.OBJECT);
   }

   @Override
   public List<? extends Config> getConfigList(String path) {
      List<ConfigObject> objects = this.getObjectList(path);
      List<Config> l = new ArrayList<>();

      for (ConfigObject o : objects) {
         l.add(o.toConfig());
      }

      return l;
   }

   @Override
   public List<? extends Object> getAnyRefList(String path) {
      List<Object> l = new ArrayList<>();

      for (ConfigValue v : this.getList(path)) {
         l.add(v.unwrapped());
      }

      return l;
   }

   @Override
   public List<Long> getBytesList(String path) {
      List<Long> l = new ArrayList<>();

      for (ConfigValue v : this.getList(path)) {
         if (v.valueType() == ConfigValueType.NUMBER) {
            l.add(((Number)v.unwrapped()).longValue());
         } else {
            if (v.valueType() != ConfigValueType.STRING) {
               throw new ConfigException.WrongType(v.origin(), path, "memory size string or number of bytes", v.valueType().name());
            }

            String s = (String)v.unwrapped();
            Long n = parseBytes(s, v.origin(), path);
            l.add(n);
         }
      }

      return l;
   }

   @Override
   public List<Long> getDurationList(String path, TimeUnit unit) {
      List<Long> l = new ArrayList<>();

      for (ConfigValue v : this.getList(path)) {
         if (v.valueType() == ConfigValueType.NUMBER) {
            Long n = unit.convert(((Number)v.unwrapped()).longValue(), TimeUnit.MILLISECONDS);
            l.add(n);
         } else {
            if (v.valueType() != ConfigValueType.STRING) {
               throw new ConfigException.WrongType(v.origin(), path, "duration string or number of milliseconds", v.valueType().name());
            }

            String s = (String)v.unwrapped();
            Long n = unit.convert(parseDuration(s, v.origin(), path), TimeUnit.NANOSECONDS);
            l.add(n);
         }
      }

      return l;
   }

   @Deprecated
   @Override
   public List<Long> getMillisecondsList(String path) {
      return this.getDurationList(path, TimeUnit.MILLISECONDS);
   }

   @Deprecated
   @Override
   public List<Long> getNanosecondsList(String path) {
      return this.getDurationList(path, TimeUnit.NANOSECONDS);
   }

   public AbstractConfigObject toFallbackValue() {
      return this.object;
   }

   public SimpleConfig withFallback(ConfigMergeable other) {
      return this.object.withFallback(other).toConfig();
   }

   @Override
   public final boolean equals(Object other) {
      return other instanceof SimpleConfig ? this.object.equals(((SimpleConfig)other).object) : false;
   }

   @Override
   public final int hashCode() {
      return 41 * this.object.hashCode();
   }

   @Override
   public String toString() {
      return "Config(" + this.object.toString() + ")";
   }

   private static String getUnits(String s) {
      int i;
      for (i = s.length() - 1; i >= 0; i--) {
         char c = s.charAt(i);
         if (!Character.isLetter(c)) {
            break;
         }
      }

      return s.substring(i + 1);
   }

   public static long parseDuration(String input, ConfigOrigin originForException, String pathForException) {
      String s = ConfigImplUtil.unicodeTrim(input);
      String originalUnitString = getUnits(s);
      String unitString = originalUnitString;
      String numberString = ConfigImplUtil.unicodeTrim(s.substring(0, s.length() - originalUnitString.length()));
      TimeUnit units = null;
      if (numberString.length() == 0) {
         throw new ConfigException.BadValue(originForException, pathForException, "No number in duration value '" + input + "'");
      } else {
         if (originalUnitString.length() > 2 && !originalUnitString.endsWith("s")) {
            unitString = originalUnitString + "s";
         }

         if (unitString.equals("") || unitString.equals("ms") || unitString.equals("milliseconds")) {
            units = TimeUnit.MILLISECONDS;
         } else if (unitString.equals("us") || unitString.equals("microseconds")) {
            units = TimeUnit.MICROSECONDS;
         } else if (unitString.equals("ns") || unitString.equals("nanoseconds")) {
            units = TimeUnit.NANOSECONDS;
         } else if (unitString.equals("d") || unitString.equals("days")) {
            units = TimeUnit.DAYS;
         } else if (unitString.equals("h") || unitString.equals("hours")) {
            units = TimeUnit.HOURS;
         } else if (!unitString.equals("s") && !unitString.equals("seconds")) {
            if (!unitString.equals("m") && !unitString.equals("minutes")) {
               throw new ConfigException.BadValue(
                  originForException, pathForException, "Could not parse time unit '" + originalUnitString + "' (try ns, us, ms, s, m, d)"
               );
            }

            units = TimeUnit.MINUTES;
         } else {
            units = TimeUnit.SECONDS;
         }

         try {
            if (numberString.matches("[0-9]+")) {
               return units.toNanos(Long.parseLong(numberString));
            } else {
               long nanosInUnit = units.toNanos(1L);
               return (long)(Double.parseDouble(numberString) * (double)nanosInUnit);
            }
         } catch (NumberFormatException var10) {
            throw new ConfigException.BadValue(originForException, pathForException, "Could not parse duration number '" + numberString + "'");
         }
      }
   }

   public static long parseBytes(String input, ConfigOrigin originForException, String pathForException) {
      String s = ConfigImplUtil.unicodeTrim(input);
      String unitString = getUnits(s);
      String numberString = ConfigImplUtil.unicodeTrim(s.substring(0, s.length() - unitString.length()));
      if (numberString.length() == 0) {
         throw new ConfigException.BadValue(originForException, pathForException, "No number in size-in-bytes value '" + input + "'");
      } else {
         SimpleConfig.MemoryUnit units = SimpleConfig.MemoryUnit.parseUnit(unitString);
         if (units == null) {
            throw new ConfigException.BadValue(
               originForException, pathForException, "Could not parse size-in-bytes unit '" + unitString + "' (try k, K, kB, KiB, kilobytes, kibibytes)"
            );
         } else {
            try {
               return numberString.matches("[0-9]+")
                  ? Long.parseLong(numberString) * units.bytes
                  : (long)(Double.parseDouble(numberString) * (double)units.bytes);
            } catch (NumberFormatException var8) {
               throw new ConfigException.BadValue(originForException, pathForException, "Could not parse size-in-bytes number '" + numberString + "'");
            }
         }
      }
   }

   private AbstractConfigValue peekPath(Path path) {
      return this.root().peekPath(path);
   }

   private static void addProblem(List<ConfigException.ValidationProblem> accumulator, Path path, ConfigOrigin origin, String problem) {
      accumulator.add(new ConfigException.ValidationProblem(path.render(), origin, problem));
   }

   private static String getDesc(ConfigValue refValue) {
      if (refValue instanceof AbstractConfigObject) {
         AbstractConfigObject obj = (AbstractConfigObject)refValue;
         return obj.isEmpty() ? "object" : "object with keys " + obj.keySet();
      } else {
         return refValue instanceof SimpleConfigList ? "list" : refValue.valueType().name().toLowerCase();
      }
   }

   private static void addMissing(List<ConfigException.ValidationProblem> accumulator, ConfigValue refValue, Path path, ConfigOrigin origin) {
      addProblem(accumulator, path, origin, "No setting at '" + path.render() + "', expecting: " + getDesc(refValue));
   }

   private static void addWrongType(List<ConfigException.ValidationProblem> accumulator, ConfigValue refValue, AbstractConfigValue actual, Path path) {
      addProblem(
         accumulator, path, actual.origin(), "Wrong value type at '" + path.render() + "', expecting: " + getDesc(refValue) + " but got: " + getDesc(actual)
      );
   }

   private static boolean couldBeNull(AbstractConfigValue v) {
      return DefaultTransformer.transform(v, ConfigValueType.NULL).valueType() == ConfigValueType.NULL;
   }

   private static boolean haveCompatibleTypes(ConfigValue reference, AbstractConfigValue value) {
      if (couldBeNull((AbstractConfigValue)reference) || couldBeNull(value)) {
         return true;
      } else if (reference instanceof AbstractConfigObject) {
         return value instanceof AbstractConfigObject;
      } else if (reference instanceof SimpleConfigList) {
         return value instanceof SimpleConfigList || value instanceof SimpleConfigObject;
      } else if (reference instanceof ConfigString) {
         return true;
      } else {
         return value instanceof ConfigString ? true : reference.valueType() == value.valueType();
      }
   }

   private static void checkValidObject(
      Path path, AbstractConfigObject reference, AbstractConfigObject value, List<ConfigException.ValidationProblem> accumulator
   ) {
      for (Entry<String, ConfigValue> entry : reference.entrySet()) {
         String key = entry.getKey();
         Path childPath;
         if (path != null) {
            childPath = Path.newKey(key).prepend(path);
         } else {
            childPath = Path.newKey(key);
         }

         AbstractConfigValue v = value.get(key);
         if (v == null) {
            addMissing(accumulator, entry.getValue(), childPath, value.origin());
         } else {
            checkValid(childPath, entry.getValue(), v, accumulator);
         }
      }
   }

   private static void checkListCompatibility(
      Path path, SimpleConfigList listRef, SimpleConfigList listValue, List<ConfigException.ValidationProblem> accumulator
   ) {
      if (!listRef.isEmpty() && !listValue.isEmpty()) {
         AbstractConfigValue refElement = listRef.get(0);

         for (ConfigValue elem : listValue) {
            AbstractConfigValue e = (AbstractConfigValue)elem;
            if (!haveCompatibleTypes(refElement, e)) {
               addProblem(
                  accumulator,
                  path,
                  e.origin(),
                  "List at '"
                     + path.render()
                     + "' contains wrong value type, expecting list of "
                     + getDesc(refElement)
                     + " but got element of type "
                     + getDesc(e)
               );
               break;
            }
         }
      }
   }

   private static void checkValid(Path path, ConfigValue reference, AbstractConfigValue value, List<ConfigException.ValidationProblem> accumulator) {
      if (haveCompatibleTypes(reference, value)) {
         if (reference instanceof AbstractConfigObject && value instanceof AbstractConfigObject) {
            checkValidObject(path, (AbstractConfigObject)reference, (AbstractConfigObject)value, accumulator);
         } else if (reference instanceof SimpleConfigList && value instanceof SimpleConfigList) {
            SimpleConfigList listRef = (SimpleConfigList)reference;
            SimpleConfigList listValue = (SimpleConfigList)value;
            checkListCompatibility(path, listRef, listValue, accumulator);
         } else if (reference instanceof SimpleConfigList && value instanceof SimpleConfigObject) {
            SimpleConfigList listRef = (SimpleConfigList)reference;
            AbstractConfigValue listValue = DefaultTransformer.transform(value, ConfigValueType.LIST);
            if (listValue instanceof SimpleConfigList) {
               checkListCompatibility(path, listRef, (SimpleConfigList)listValue, accumulator);
            } else {
               addWrongType(accumulator, reference, value, path);
            }
         }
      } else {
         addWrongType(accumulator, reference, value, path);
      }
   }

   @Override
   public boolean isResolved() {
      return this.root().resolveStatus() == ResolveStatus.RESOLVED;
   }

   @Override
   public void checkValid(Config reference, String... restrictToPaths) {
      SimpleConfig ref = (SimpleConfig)reference;
      if (ref.root().resolveStatus() != ResolveStatus.RESOLVED) {
         throw new ConfigException.BugOrBroken(
            "do not call checkValid() with an unresolved reference config, call Config#resolve(), see Config#resolve() API docs"
         );
      } else if (this.root().resolveStatus() != ResolveStatus.RESOLVED) {
         throw new ConfigException.NotResolved("need to Config#resolve() each config before using it, see the API docs for Config#resolve()");
      } else {
         List<ConfigException.ValidationProblem> problems = new ArrayList<>();
         if (restrictToPaths.length == 0) {
            checkValidObject(null, ref.root(), this.root(), problems);
         } else {
            for (String p : restrictToPaths) {
               Path path = Path.newPath(p);
               AbstractConfigValue refValue = ref.peekPath(path);
               if (refValue != null) {
                  AbstractConfigValue child = this.peekPath(path);
                  if (child != null) {
                     checkValid(path, refValue, child, problems);
                  } else {
                     addMissing(problems, refValue, path, this.origin());
                  }
               }
            }
         }

         if (!problems.isEmpty()) {
            throw new ConfigException.ValidationFailed(problems);
         }
      }
   }

   public SimpleConfig withOnlyPath(String pathExpression) {
      Path path = Path.newPath(pathExpression);
      return new SimpleConfig(this.root().withOnlyPath(path));
   }

   public SimpleConfig withoutPath(String pathExpression) {
      Path path = Path.newPath(pathExpression);
      return new SimpleConfig(this.root().withoutPath(path));
   }

   public SimpleConfig withValue(String pathExpression, ConfigValue v) {
      Path path = Path.newPath(pathExpression);
      return new SimpleConfig(this.root().withValue(path, v));
   }

   SimpleConfig atKey(ConfigOrigin origin, String key) {
      return this.root().atKey(origin, key);
   }

   public SimpleConfig atKey(String key) {
      return this.root().atKey(key);
   }

   @Override
   public Config atPath(String path) {
      return this.root().atPath(path);
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SerializedConfigValue(this);
   }

   private static enum MemoryUnit {
      BYTES("", 1024, 0),
      KILOBYTES("kilo", 1000, 1),
      MEGABYTES("mega", 1000, 2),
      GIGABYTES("giga", 1000, 3),
      TERABYTES("tera", 1000, 4),
      PETABYTES("peta", 1000, 5),
      EXABYTES("exa", 1000, 6),
      ZETTABYTES("zetta", 1000, 7),
      YOTTABYTES("yotta", 1000, 8),
      KIBIBYTES("kibi", 1024, 1),
      MEBIBYTES("mebi", 1024, 2),
      GIBIBYTES("gibi", 1024, 3),
      TEBIBYTES("tebi", 1024, 4),
      PEBIBYTES("pebi", 1024, 5),
      EXBIBYTES("exbi", 1024, 6),
      ZEBIBYTES("zebi", 1024, 7),
      YOBIBYTES("yobi", 1024, 8);

      final String prefix;
      final int powerOf;
      final int power;
      final long bytes;
      private static Map<String, SimpleConfig.MemoryUnit> unitsMap = makeUnitsMap();

      private MemoryUnit(String prefix, int powerOf, int power) {
         this.prefix = prefix;
         this.powerOf = powerOf;
         this.power = power;
         int i = power;

         long bytes;
         for (bytes = 1L; i > 0; i--) {
            bytes *= (long)powerOf;
         }

         this.bytes = bytes;
      }

      private static Map<String, SimpleConfig.MemoryUnit> makeUnitsMap() {
         Map<String, SimpleConfig.MemoryUnit> map = new HashMap<>();

         for (SimpleConfig.MemoryUnit unit : values()) {
            map.put(unit.prefix + "byte", unit);
            map.put(unit.prefix + "bytes", unit);
            if (unit.prefix.length() == 0) {
               map.put("b", unit);
               map.put("B", unit);
               map.put("", unit);
            } else {
               String first = unit.prefix.substring(0, 1);
               String firstUpper = first.toUpperCase();
               if (unit.powerOf == 1024) {
                  map.put(first, unit);
                  map.put(firstUpper, unit);
                  map.put(firstUpper + "i", unit);
                  map.put(firstUpper + "iB", unit);
               } else {
                  if (unit.powerOf != 1000) {
                     throw new RuntimeException("broken MemoryUnit enum");
                  }

                  if (unit.power == 1) {
                     map.put(first + "B", unit);
                  } else {
                     map.put(firstUpper + "B", unit);
                  }
               }
            }
         }

         return map;
      }

      static SimpleConfig.MemoryUnit parseUnit(String unit) {
         return unitsMap.get(unit);
      }
   }
}
