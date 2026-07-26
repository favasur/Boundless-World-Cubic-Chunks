package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigIncluder;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigParseable;
import com.typesafe.config.ConfigValue;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class ConfigImpl {
   private static final ConfigOrigin defaultValueOrigin = SimpleConfigOrigin.newSimple("hardcoded value");
   private static final ConfigBoolean defaultTrueValue = new ConfigBoolean(defaultValueOrigin, true);
   private static final ConfigBoolean defaultFalseValue = new ConfigBoolean(defaultValueOrigin, false);
   private static final ConfigNull defaultNullValue = new ConfigNull(defaultValueOrigin);
   private static final SimpleConfigList defaultEmptyList = new SimpleConfigList(defaultValueOrigin, Collections.emptyList());
   private static final SimpleConfigObject defaultEmptyObject = SimpleConfigObject.empty(defaultValueOrigin);

   public ConfigImpl() {
   }

   public static Config computeCachedConfig(ClassLoader loader, String key, Callable<Config> updater) {
      ConfigImpl.LoaderCache cache;
      try {
         cache = ConfigImpl.LoaderCacheHolder.cache;
      } catch (ExceptionInInitializerError var5) {
         throw ConfigImplUtil.extractInitializerError(var5);
      }

      return cache.getOrElseUpdate(loader, key, updater);
   }

   public static ConfigObject parseResourcesAnySyntax(Class<?> klass, String resourceBasename, ConfigParseOptions baseOptions) {
      SimpleIncluder.NameSource source = new ConfigImpl.ClasspathNameSourceWithClass(klass);
      return SimpleIncluder.fromBasename(source, resourceBasename, baseOptions);
   }

   public static ConfigObject parseResourcesAnySyntax(String resourceBasename, ConfigParseOptions baseOptions) {
      SimpleIncluder.NameSource source = new ConfigImpl.ClasspathNameSource();
      return SimpleIncluder.fromBasename(source, resourceBasename, baseOptions);
   }

   public static ConfigObject parseFileAnySyntax(File basename, ConfigParseOptions baseOptions) {
      SimpleIncluder.NameSource source = new ConfigImpl.FileNameSource();
      return SimpleIncluder.fromBasename(source, basename.getPath(), baseOptions);
   }

   static AbstractConfigObject emptyObject(String originDescription) {
      ConfigOrigin origin = originDescription != null ? SimpleConfigOrigin.newSimple(originDescription) : null;
      return emptyObject(origin);
   }

   public static Config emptyConfig(String originDescription) {
      return emptyObject(originDescription).toConfig();
   }

   static AbstractConfigObject empty(ConfigOrigin origin) {
      return emptyObject(origin);
   }

   private static SimpleConfigList emptyList(ConfigOrigin origin) {
      return origin != null && origin != defaultValueOrigin ? new SimpleConfigList(origin, Collections.emptyList()) : defaultEmptyList;
   }

   private static AbstractConfigObject emptyObject(ConfigOrigin origin) {
      return origin == defaultValueOrigin ? defaultEmptyObject : SimpleConfigObject.empty(origin);
   }

   private static ConfigOrigin valueOrigin(String originDescription) {
      return (ConfigOrigin)(originDescription == null ? defaultValueOrigin : SimpleConfigOrigin.newSimple(originDescription));
   }

   public static ConfigValue fromAnyRef(Object object, String originDescription) {
      ConfigOrigin origin = valueOrigin(originDescription);
      return fromAnyRef(object, origin, FromMapMode.KEYS_ARE_KEYS);
   }

   public static ConfigObject fromPathMap(Map<String, ? extends Object> pathMap, String originDescription) {
      ConfigOrigin origin = valueOrigin(originDescription);
      return (ConfigObject)fromAnyRef(pathMap, origin, FromMapMode.KEYS_ARE_PATHS);
   }

   static AbstractConfigValue fromAnyRef(Object object, ConfigOrigin origin, FromMapMode mapMode) {
      if (origin == null) {
         throw new ConfigException.BugOrBroken("origin not supposed to be null");
      } else if (object == null) {
         return origin != defaultValueOrigin ? new ConfigNull(origin) : defaultNullValue;
      } else if (object instanceof Boolean) {
         if (origin != defaultValueOrigin) {
            return new ConfigBoolean(origin, (Boolean)object);
         } else {
            return (Boolean)object ? defaultTrueValue : defaultFalseValue;
         }
      } else if (object instanceof String) {
         return new ConfigString(origin, (String)object);
      } else if (object instanceof Number) {
         if (object instanceof Double) {
            return new ConfigDouble(origin, (Double)object, null);
         } else if (object instanceof Integer) {
            return new ConfigInt(origin, (Integer)object, null);
         } else {
            return (AbstractConfigValue)(object instanceof Long
               ? new ConfigLong(origin, (Long)object, null)
               : ConfigNumber.newNumber(origin, ((Number)object).doubleValue(), null));
         }
      } else if (object instanceof Map) {
         if (((Map)object).isEmpty()) {
            return emptyObject(origin);
         } else if (mapMode == FromMapMode.KEYS_ARE_KEYS) {
            Map<String, AbstractConfigValue> values = new HashMap<>();

            for (Entry<?, ?> entry : ((Map)object).entrySet()) {
               Object key = entry.getKey();
               if (!(key instanceof String)) {
                  throw new ConfigException.BugOrBroken("bug in method caller: not valid to create ConfigObject from map with non-String key: " + key);
               }

               AbstractConfigValue value = fromAnyRef(entry.getValue(), origin, mapMode);
               values.put((String)key, value);
            }

            return new SimpleConfigObject(origin, values);
         } else {
            return PropertiesParser.fromPathMap(origin, (Map<?, ?>)object);
         }
      } else if (!(object instanceof Iterable)) {
         throw new ConfigException.BugOrBroken("bug in method caller: not valid to create ConfigValue from: " + object);
      } else {
         Iterator<?> i = ((Iterable)object).iterator();
         if (!i.hasNext()) {
            return emptyList(origin);
         } else {
            List<AbstractConfigValue> values = new ArrayList<>();

            while (i.hasNext()) {
               AbstractConfigValue v = fromAnyRef(i.next(), origin, mapMode);
               values.add(v);
            }

            return new SimpleConfigList(origin, values);
         }
      }
   }

   static ConfigIncluder defaultIncluder() {
      try {
         return ConfigImpl.DefaultIncluderHolder.defaultIncluder;
      } catch (ExceptionInInitializerError var1) {
         throw ConfigImplUtil.extractInitializerError(var1);
      }
   }

   private static Properties getSystemProperties() {
      Properties systemProperties = System.getProperties();
      Properties systemPropertiesCopy = new Properties();
      synchronized (systemProperties) {
         systemPropertiesCopy.putAll(systemProperties);
         return systemPropertiesCopy;
      }
   }

   private static AbstractConfigObject loadSystemProperties() {
      return (AbstractConfigObject)Parseable.newProperties(getSystemProperties(), ConfigParseOptions.defaults().setOriginDescription("system properties"))
         .parse();
   }

   static AbstractConfigObject systemPropertiesAsConfigObject() {
      try {
         return ConfigImpl.SystemPropertiesHolder.systemProperties;
      } catch (ExceptionInInitializerError var1) {
         throw ConfigImplUtil.extractInitializerError(var1);
      }
   }

   public static Config systemPropertiesAsConfig() {
      return systemPropertiesAsConfigObject().toConfig();
   }

   public static void reloadSystemPropertiesConfig() {
      ConfigImpl.SystemPropertiesHolder.systemProperties = loadSystemProperties();
   }

   private static AbstractConfigObject loadEnvVariables() {
      Map<String, String> env = System.getenv();
      Map<String, AbstractConfigValue> m = new HashMap<>();

      for (Entry<String, String> entry : env.entrySet()) {
         String key = entry.getKey();
         m.put(key, new ConfigString(SimpleConfigOrigin.newSimple("env var " + key), entry.getValue()));
      }

      return new SimpleConfigObject(SimpleConfigOrigin.newSimple("env variables"), m, ResolveStatus.RESOLVED, false);
   }

   static AbstractConfigObject envVariablesAsConfigObject() {
      try {
         return ConfigImpl.EnvVariablesHolder.envVariables;
      } catch (ExceptionInInitializerError var1) {
         throw ConfigImplUtil.extractInitializerError(var1);
      }
   }

   public static Config envVariablesAsConfig() {
      return envVariablesAsConfigObject().toConfig();
   }

   public static Config defaultReference(final ClassLoader loader) {
      return computeCachedConfig(loader, "defaultReference", new Callable<Config>() {
         public Config call() {
            Config unresolvedResources = Parseable.newResources("reference.conf", ConfigParseOptions.defaults().setClassLoader(loader)).parse().toConfig();
            return ConfigImpl.systemPropertiesAsConfig().withFallback(unresolvedResources).resolve();
         }
      });
   }

   public static boolean traceLoadsEnabled() {
      try {
         return ConfigImpl.DebugHolder.traceLoadsEnabled();
      } catch (ExceptionInInitializerError var1) {
         throw ConfigImplUtil.extractInitializerError(var1);
      }
   }

   public static void trace(String message) {
      System.err.println(message);
   }

   static ConfigException.NotResolved improveNotResolved(Path what, ConfigException.NotResolved original) {
      String newMessage = what.render() + " has not been resolved, you need to call Config#resolve()," + " see API docs for Config#resolve()";
      return newMessage.equals(original.getMessage()) ? original : new ConfigException.NotResolved(newMessage, original);
   }

   static class ClasspathNameSource implements SimpleIncluder.NameSource {
      ClasspathNameSource() {
      }

      @Override
      public ConfigParseable nameToParseable(String name, ConfigParseOptions parseOptions) {
         return Parseable.newResources(name, parseOptions);
      }
   }

   static class ClasspathNameSourceWithClass implements SimpleIncluder.NameSource {
      private final Class<?> klass;

      public ClasspathNameSourceWithClass(Class<?> klass) {
         this.klass = klass;
      }

      @Override
      public ConfigParseable nameToParseable(String name, ConfigParseOptions parseOptions) {
         return Parseable.newResources(this.klass, name, parseOptions);
      }
   }

   private static class DebugHolder {
      private static String LOADS = "loads";
      private static final Map<String, Boolean> diagnostics = loadDiagnostics();
      private static final boolean traceLoadsEnabled = diagnostics.get(LOADS);

      private DebugHolder() {
      }

      private static Map<String, Boolean> loadDiagnostics() {
         Map<String, Boolean> result = new HashMap<>();
         result.put(LOADS, false);
         String s = System.getProperty("config.trace");
         if (s == null) {
            return result;
         } else {
            String[] keys = s.split(",");

            for (String k : keys) {
               if (k.equals(LOADS)) {
                  result.put(LOADS, true);
               } else {
                  System.err.println("config.trace property contains unknown trace topic '" + k + "'");
               }
            }

            return result;
         }
      }

      static boolean traceLoadsEnabled() {
         return traceLoadsEnabled;
      }
   }

   private static class DefaultIncluderHolder {
      static final ConfigIncluder defaultIncluder = new SimpleIncluder(null);

      private DefaultIncluderHolder() {
      }
   }

   private static class EnvVariablesHolder {
      static final AbstractConfigObject envVariables = ConfigImpl.loadEnvVariables();

      private EnvVariablesHolder() {
      }
   }

   static class FileNameSource implements SimpleIncluder.NameSource {
      FileNameSource() {
      }

      @Override
      public ConfigParseable nameToParseable(String name, ConfigParseOptions parseOptions) {
         return Parseable.newFile(new File(name), parseOptions);
      }
   }

   private static class LoaderCache {
      private Config currentSystemProperties = null;
      private ClassLoader currentLoader = null;
      private Map<String, Config> cache = new HashMap<>();

      LoaderCache() {
      }

      synchronized Config getOrElseUpdate(ClassLoader loader, String key, Callable<Config> updater) {
         if (loader != this.currentLoader) {
            this.cache.clear();
            this.currentLoader = loader;
         }

         Config systemProperties = ConfigImpl.systemPropertiesAsConfig();
         if (systemProperties != this.currentSystemProperties) {
            this.cache.clear();
            this.currentSystemProperties = systemProperties;
         }

         Config config = this.cache.get(key);
         if (config == null) {
            try {
               config = updater.call();
            } catch (RuntimeException var7) {
               throw var7;
            } catch (Exception var8) {
               throw new ConfigException.Generic(var8.getMessage(), var8);
            }

            if (config == null) {
               throw new ConfigException.BugOrBroken("null config from cache updater");
            }

            this.cache.put(key, config);
         }

         return config;
      }
   }

   private static class LoaderCacheHolder {
      static final ConfigImpl.LoaderCache cache = new ConfigImpl.LoaderCache();

      private LoaderCacheHolder() {
      }
   }

   private static class SystemPropertiesHolder {
      static volatile AbstractConfigObject systemProperties = ConfigImpl.loadSystemProperties();

      private SystemPropertiesHolder() {
      }
   }
}
