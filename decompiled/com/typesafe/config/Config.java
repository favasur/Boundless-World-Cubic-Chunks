package com.typesafe.config;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public interface Config extends ConfigMergeable {
   ConfigObject root();

   ConfigOrigin origin();

   Config withFallback(ConfigMergeable var1);

   Config resolve();

   Config resolve(ConfigResolveOptions var1);

   boolean isResolved();

   Config resolveWith(Config var1);

   Config resolveWith(Config var1, ConfigResolveOptions var2);

   void checkValid(Config var1, String... var2);

   boolean hasPath(String var1);

   boolean isEmpty();

   Set<Entry<String, ConfigValue>> entrySet();

   boolean getBoolean(String var1);

   Number getNumber(String var1);

   int getInt(String var1);

   long getLong(String var1);

   double getDouble(String var1);

   String getString(String var1);

   ConfigObject getObject(String var1);

   Config getConfig(String var1);

   Object getAnyRef(String var1);

   ConfigValue getValue(String var1);

   Long getBytes(String var1);

   @Deprecated
   Long getMilliseconds(String var1);

   @Deprecated
   Long getNanoseconds(String var1);

   long getDuration(String var1, TimeUnit var2);

   ConfigList getList(String var1);

   List<Boolean> getBooleanList(String var1);

   List<Number> getNumberList(String var1);

   List<Integer> getIntList(String var1);

   List<Long> getLongList(String var1);

   List<Double> getDoubleList(String var1);

   List<String> getStringList(String var1);

   List<? extends ConfigObject> getObjectList(String var1);

   List<? extends Config> getConfigList(String var1);

   List<? extends Object> getAnyRefList(String var1);

   List<Long> getBytesList(String var1);

   @Deprecated
   List<Long> getMillisecondsList(String var1);

   @Deprecated
   List<Long> getNanosecondsList(String var1);

   List<Long> getDurationList(String var1, TimeUnit var2);

   Config withOnlyPath(String var1);

   Config withoutPath(String var1);

   Config atPath(String var1);

   Config atKey(String var1);

   Config withValue(String var1, ConfigValue var2);
}
