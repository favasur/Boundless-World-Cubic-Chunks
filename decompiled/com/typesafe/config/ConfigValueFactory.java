package com.typesafe.config;

import com.typesafe.config.impl.ConfigImpl;
import java.util.Map;

public final class ConfigValueFactory {
   private ConfigValueFactory() {
   }

   public static ConfigValue fromAnyRef(Object object, String originDescription) {
      return ConfigImpl.fromAnyRef(object, originDescription);
   }

   public static ConfigObject fromMap(Map<String, ? extends Object> values, String originDescription) {
      return (ConfigObject)fromAnyRef(values, originDescription);
   }

   public static ConfigList fromIterable(Iterable<? extends Object> values, String originDescription) {
      return (ConfigList)fromAnyRef(values, originDescription);
   }

   public static ConfigValue fromAnyRef(Object object) {
      return fromAnyRef(object, null);
   }

   public static ConfigObject fromMap(Map<String, ? extends Object> values) {
      return fromMap(values, null);
   }

   public static ConfigList fromIterable(Iterable<? extends Object> values) {
      return fromIterable(values, null);
   }
}
