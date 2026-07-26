package com.typesafe.config;

import java.util.Map;

public interface ConfigObject extends ConfigValue, Map<String, ConfigValue> {
   Config toConfig();

   Map<String, Object> unwrapped();

   ConfigObject withFallback(ConfigMergeable var1);

   ConfigValue get(Object var1);

   ConfigObject withOnlyKey(String var1);

   ConfigObject withoutKey(String var1);

   ConfigObject withValue(String var1, ConfigValue var2);
}
