package com.typesafe.config;

import com.typesafe.config.impl.ConfigImpl;
import com.typesafe.config.impl.Parseable;
import java.io.File;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public final class ConfigFactory {
   private ConfigFactory() {
   }

   public static Config load(String resourceBasename) {
      return load(resourceBasename, ConfigParseOptions.defaults(), ConfigResolveOptions.defaults());
   }

   public static Config load(ClassLoader loader, String resourceBasename) {
      return load(resourceBasename, ConfigParseOptions.defaults().setClassLoader(loader), ConfigResolveOptions.defaults());
   }

   public static Config load(String resourceBasename, ConfigParseOptions parseOptions, ConfigResolveOptions resolveOptions) {
      Config appConfig = parseResourcesAnySyntax(resourceBasename, parseOptions);
      return load(parseOptions.getClassLoader(), appConfig, resolveOptions);
   }

   public static Config load(ClassLoader loader, String resourceBasename, ConfigParseOptions parseOptions, ConfigResolveOptions resolveOptions) {
      return load(resourceBasename, parseOptions.setClassLoader(loader), resolveOptions);
   }

   public static Config load(Config config) {
      return load(Thread.currentThread().getContextClassLoader(), config);
   }

   public static Config load(ClassLoader loader, Config config) {
      return load(loader, config, ConfigResolveOptions.defaults());
   }

   public static Config load(Config config, ConfigResolveOptions resolveOptions) {
      return load(Thread.currentThread().getContextClassLoader(), config, resolveOptions);
   }

   public static Config load(ClassLoader loader, Config config, ConfigResolveOptions resolveOptions) {
      return defaultOverrides(loader).withFallback(config).withFallback(defaultReference(loader)).resolve(resolveOptions);
   }

   private static Config loadDefaultConfig(ClassLoader loader) {
      return loadDefaultConfig(loader, ConfigParseOptions.defaults());
   }

   private static Config loadDefaultConfig(ClassLoader loader, ConfigParseOptions parseOptions) {
      return loadDefaultConfig(loader, parseOptions, ConfigResolveOptions.defaults());
   }

   private static Config loadDefaultConfig(ClassLoader loader, ConfigResolveOptions resolveOptions) {
      return loadDefaultConfig(loader, ConfigParseOptions.defaults(), resolveOptions);
   }

   private static Config loadDefaultConfig(ClassLoader loader, ConfigParseOptions parseOptions, ConfigResolveOptions resolveOptions) {
      int specified = 0;
      String resource = System.getProperty("config.resource");
      if (resource != null) {
         specified++;
      }

      String file = System.getProperty("config.file");
      if (file != null) {
         specified++;
      }

      String url = System.getProperty("config.url");
      if (url != null) {
         specified++;
      }

      if (specified == 0) {
         return load(loader, "application", parseOptions, resolveOptions);
      } else if (specified > 1) {
         throw new ConfigException.Generic(
            "You set more than one of config.file='" + file + "', config.url='" + url + "', config.resource='" + resource + "'; don't know which one to use!"
         );
      } else {
         ConfigParseOptions overrideOptions = parseOptions.setAllowMissing(false);
         if (resource != null) {
            if (resource.startsWith("/")) {
               resource = resource.substring(1);
            }

            Config parsedResources = parseResources(loader, resource, overrideOptions);
            return load(loader, parsedResources, resolveOptions);
         } else if (file != null) {
            Config parsedFile = parseFile(new File(file), overrideOptions);
            return load(loader, parsedFile, resolveOptions);
         } else {
            try {
               Config parsedURL = parseURL(new URL(url), overrideOptions);
               return load(loader, parsedURL, resolveOptions);
            } catch (MalformedURLException var9) {
               throw new ConfigException.Generic("Bad URL in config.url system property: '" + url + "': " + var9.getMessage(), var9);
            }
         }
      }
   }

   public static Config load() {
      return load(Thread.currentThread().getContextClassLoader());
   }

   public static Config load(ConfigParseOptions parseOptions) {
      return load(Thread.currentThread().getContextClassLoader(), parseOptions);
   }

   public static Config load(final ClassLoader loader) {
      return ConfigImpl.computeCachedConfig(loader, "load", new Callable<Config>() {
         public Config call() {
            return ConfigFactory.loadDefaultConfig(loader);
         }
      });
   }

   public static Config load(ClassLoader loader, ConfigParseOptions parseOptions) {
      return loadDefaultConfig(loader, parseOptions);
   }

   public static Config load(ClassLoader loader, ConfigResolveOptions resolveOptions) {
      return loadDefaultConfig(loader, resolveOptions);
   }

   public static Config load(ClassLoader loader, ConfigParseOptions parseOptions, ConfigResolveOptions resolveOptions) {
      return loadDefaultConfig(loader, parseOptions, resolveOptions);
   }

   public static Config defaultReference() {
      return defaultReference(Thread.currentThread().getContextClassLoader());
   }

   public static Config defaultReference(ClassLoader loader) {
      return ConfigImpl.defaultReference(loader);
   }

   public static Config defaultOverrides() {
      return systemProperties();
   }

   public static Config defaultOverrides(ClassLoader loader) {
      return systemProperties();
   }

   public static void invalidateCaches() {
      ConfigImpl.reloadSystemPropertiesConfig();
   }

   public static Config empty() {
      return empty(null);
   }

   public static Config empty(String originDescription) {
      return ConfigImpl.emptyConfig(originDescription);
   }

   public static Config systemProperties() {
      return ConfigImpl.systemPropertiesAsConfig();
   }

   public static Config systemEnvironment() {
      return ConfigImpl.envVariablesAsConfig();
   }

   public static Config parseProperties(Properties properties, ConfigParseOptions options) {
      return Parseable.newProperties(properties, options).parse().toConfig();
   }

   public static Config parseProperties(Properties properties) {
      return parseProperties(properties, ConfigParseOptions.defaults());
   }

   public static Config parseReader(Reader reader, ConfigParseOptions options) {
      return Parseable.newReader(reader, options).parse().toConfig();
   }

   public static Config parseReader(Reader reader) {
      return parseReader(reader, ConfigParseOptions.defaults());
   }

   public static Config parseURL(URL url, ConfigParseOptions options) {
      return Parseable.newURL(url, options).parse().toConfig();
   }

   public static Config parseURL(URL url) {
      return parseURL(url, ConfigParseOptions.defaults());
   }

   public static Config parseFile(File file, ConfigParseOptions options) {
      return Parseable.newFile(file, options).parse().toConfig();
   }

   public static Config parseFile(File file) {
      return parseFile(file, ConfigParseOptions.defaults());
   }

   public static Config parseFileAnySyntax(File fileBasename, ConfigParseOptions options) {
      return ConfigImpl.parseFileAnySyntax(fileBasename, options).toConfig();
   }

   public static Config parseFileAnySyntax(File fileBasename) {
      return parseFileAnySyntax(fileBasename, ConfigParseOptions.defaults());
   }

   public static Config parseResources(Class<?> klass, String resource, ConfigParseOptions options) {
      return Parseable.newResources(klass, resource, options).parse().toConfig();
   }

   public static Config parseResources(Class<?> klass, String resource) {
      return parseResources(klass, resource, ConfigParseOptions.defaults());
   }

   public static Config parseResourcesAnySyntax(Class<?> klass, String resourceBasename, ConfigParseOptions options) {
      return ConfigImpl.parseResourcesAnySyntax(klass, resourceBasename, options).toConfig();
   }

   public static Config parseResourcesAnySyntax(Class<?> klass, String resourceBasename) {
      return parseResourcesAnySyntax(klass, resourceBasename, ConfigParseOptions.defaults());
   }

   public static Config parseResources(ClassLoader loader, String resource, ConfigParseOptions options) {
      return Parseable.newResources(resource, options.setClassLoader(loader)).parse().toConfig();
   }

   public static Config parseResources(ClassLoader loader, String resource) {
      return parseResources(loader, resource, ConfigParseOptions.defaults());
   }

   public static Config parseResourcesAnySyntax(ClassLoader loader, String resourceBasename, ConfigParseOptions options) {
      return ConfigImpl.parseResourcesAnySyntax(resourceBasename, options.setClassLoader(loader)).toConfig();
   }

   public static Config parseResourcesAnySyntax(ClassLoader loader, String resourceBasename) {
      return parseResourcesAnySyntax(loader, resourceBasename, ConfigParseOptions.defaults());
   }

   public static Config parseResources(String resource, ConfigParseOptions options) {
      return Parseable.newResources(resource, options).parse().toConfig();
   }

   public static Config parseResources(String resource) {
      return parseResources(resource, ConfigParseOptions.defaults());
   }

   public static Config parseResourcesAnySyntax(String resourceBasename, ConfigParseOptions options) {
      return ConfigImpl.parseResourcesAnySyntax(resourceBasename, options).toConfig();
   }

   public static Config parseResourcesAnySyntax(String resourceBasename) {
      return parseResourcesAnySyntax(resourceBasename, ConfigParseOptions.defaults());
   }

   public static Config parseString(String s, ConfigParseOptions options) {
      return Parseable.newString(s, options).parse().toConfig();
   }

   public static Config parseString(String s) {
      return parseString(s, ConfigParseOptions.defaults());
   }

   public static Config parseMap(Map<String, ? extends Object> values, String originDescription) {
      return ConfigImpl.fromPathMap(values, originDescription).toConfig();
   }

   public static Config parseMap(Map<String, ? extends Object> values) {
      return parseMap(values, null);
   }
}
