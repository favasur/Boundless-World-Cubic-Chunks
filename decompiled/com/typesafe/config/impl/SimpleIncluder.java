package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigIncluder;
import com.typesafe.config.ConfigIncluderClasspath;
import com.typesafe.config.ConfigIncluderFile;
import com.typesafe.config.ConfigIncluderURL;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigParseable;
import com.typesafe.config.ConfigSyntax;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class SimpleIncluder implements FullIncluder {
   private ConfigIncluder fallback;

   SimpleIncluder(ConfigIncluder fallback) {
      this.fallback = fallback;
   }

   static ConfigParseOptions clearForInclude(ConfigParseOptions options) {
      return options.setSyntax(null).setOriginDescription(null).setAllowMissing(true);
   }

   @Override
   public ConfigObject include(ConfigIncludeContext context, String name) {
      ConfigObject obj = includeWithoutFallback(context, name);
      return this.fallback != null ? obj.withFallback(this.fallback.include(context, name)) : obj;
   }

   static ConfigObject includeWithoutFallback(ConfigIncludeContext context, String name) {
      URL url;
      try {
         url = new URL(name);
      } catch (MalformedURLException var4) {
         url = null;
      }

      if (url != null) {
         return includeURLWithoutFallback(context, url);
      } else {
         SimpleIncluder.NameSource source = new SimpleIncluder.RelativeNameSource(context);
         return fromBasename(source, name, context.parseOptions());
      }
   }

   @Override
   public ConfigObject includeURL(ConfigIncludeContext context, URL url) {
      ConfigObject obj = includeURLWithoutFallback(context, url);
      return this.fallback != null && this.fallback instanceof ConfigIncluderURL
         ? obj.withFallback(((ConfigIncluderURL)this.fallback).includeURL(context, url))
         : obj;
   }

   static ConfigObject includeURLWithoutFallback(ConfigIncludeContext context, URL url) {
      return ConfigFactory.parseURL(url, context.parseOptions()).root();
   }

   @Override
   public ConfigObject includeFile(ConfigIncludeContext context, File file) {
      ConfigObject obj = includeFileWithoutFallback(context, file);
      return this.fallback != null && this.fallback instanceof ConfigIncluderFile
         ? obj.withFallback(((ConfigIncluderFile)this.fallback).includeFile(context, file))
         : obj;
   }

   static ConfigObject includeFileWithoutFallback(ConfigIncludeContext context, File file) {
      return ConfigFactory.parseFileAnySyntax(file, context.parseOptions()).root();
   }

   @Override
   public ConfigObject includeResources(ConfigIncludeContext context, String resource) {
      ConfigObject obj = includeResourceWithoutFallback(context, resource);
      return this.fallback != null && this.fallback instanceof ConfigIncluderClasspath
         ? obj.withFallback(((ConfigIncluderClasspath)this.fallback).includeResources(context, resource))
         : obj;
   }

   static ConfigObject includeResourceWithoutFallback(ConfigIncludeContext context, String resource) {
      return ConfigFactory.parseResourcesAnySyntax(resource, context.parseOptions()).root();
   }

   @Override
   public ConfigIncluder withFallback(ConfigIncluder fallback) {
      if (this == fallback) {
         throw new ConfigException.BugOrBroken("trying to create includer cycle");
      } else if (this.fallback == fallback) {
         return this;
      } else {
         return this.fallback != null ? new SimpleIncluder(this.fallback.withFallback(fallback)) : new SimpleIncluder(fallback);
      }
   }

   static ConfigObject fromBasename(SimpleIncluder.NameSource source, String name, ConfigParseOptions options) {
      ConfigObject obj;
      if (!name.endsWith(".conf") && !name.endsWith(".json") && !name.endsWith(".properties")) {
         ConfigParseable confHandle = source.nameToParseable(name + ".conf", options);
         ConfigParseable jsonHandle = source.nameToParseable(name + ".json", options);
         ConfigParseable propsHandle = source.nameToParseable(name + ".properties", options);
         boolean gotSomething = false;
         List<ConfigException.IO> fails = new ArrayList<>();
         ConfigSyntax syntax = options.getSyntax();
         obj = SimpleConfigObject.empty(SimpleConfigOrigin.newSimple(name));
         if (syntax == null || syntax == ConfigSyntax.CONF) {
            try {
               obj = confHandle.parse(confHandle.options().setAllowMissing(false).setSyntax(ConfigSyntax.CONF));
               gotSomething = true;
            } catch (ConfigException.IO var15) {
               fails.add(var15);
            }
         }

         if (syntax == null || syntax == ConfigSyntax.JSON) {
            try {
               ConfigObject parsed = jsonHandle.parse(jsonHandle.options().setAllowMissing(false).setSyntax(ConfigSyntax.JSON));
               obj = obj.withFallback(parsed);
               gotSomething = true;
            } catch (ConfigException.IO var14) {
               fails.add(var14);
            }
         }

         if (syntax == null || syntax == ConfigSyntax.PROPERTIES) {
            try {
               ConfigObject parsed = propsHandle.parse(propsHandle.options().setAllowMissing(false).setSyntax(ConfigSyntax.PROPERTIES));
               obj = obj.withFallback(parsed);
               gotSomething = true;
            } catch (ConfigException.IO var13) {
               fails.add(var13);
            }
         }

         if (!options.getAllowMissing() && !gotSomething) {
            if (ConfigImpl.traceLoadsEnabled()) {
               ConfigImpl.trace("Did not find '" + name + "' with any extension (.conf, .json, .properties); " + "exceptions should have been logged above.");
            }

            if (fails.isEmpty()) {
               throw new ConfigException.BugOrBroken("should not be reached: nothing found but no exceptions thrown");
            }

            StringBuilder sb = new StringBuilder();

            for (Throwable t : fails) {
               sb.append(t.getMessage());
               sb.append(", ");
            }

            sb.setLength(sb.length() - 2);
            throw new ConfigException.IO(SimpleConfigOrigin.newSimple(name), sb.toString(), fails.get(0));
         }

         if (!gotSomething && ConfigImpl.traceLoadsEnabled()) {
            ConfigImpl.trace(
               "Did not find '"
                  + name
                  + "' with any extension (.conf, .json, .properties); but '"
                  + name
                  + "' is allowed to be missing. Exceptions from load attempts should have been logged above."
            );
         }
      } else {
         ConfigParseable p = source.nameToParseable(name, options);
         obj = p.parse(p.options().setAllowMissing(options.getAllowMissing()));
      }

      return obj;
   }

   static FullIncluder makeFull(ConfigIncluder includer) {
      return (FullIncluder)(includer instanceof FullIncluder ? (FullIncluder)includer : new SimpleIncluder.Proxy(includer));
   }

   interface NameSource {
      ConfigParseable nameToParseable(String var1, ConfigParseOptions var2);
   }

   private static class Proxy implements FullIncluder {
      final ConfigIncluder delegate;

      Proxy(ConfigIncluder delegate) {
         this.delegate = delegate;
      }

      @Override
      public ConfigIncluder withFallback(ConfigIncluder fallback) {
         return this;
      }

      @Override
      public ConfigObject include(ConfigIncludeContext context, String what) {
         return this.delegate.include(context, what);
      }

      @Override
      public ConfigObject includeResources(ConfigIncludeContext context, String what) {
         return this.delegate instanceof ConfigIncluderClasspath
            ? ((ConfigIncluderClasspath)this.delegate).includeResources(context, what)
            : SimpleIncluder.includeResourceWithoutFallback(context, what);
      }

      @Override
      public ConfigObject includeURL(ConfigIncludeContext context, URL what) {
         return this.delegate instanceof ConfigIncluderURL
            ? ((ConfigIncluderURL)this.delegate).includeURL(context, what)
            : SimpleIncluder.includeURLWithoutFallback(context, what);
      }

      @Override
      public ConfigObject includeFile(ConfigIncludeContext context, File what) {
         return this.delegate instanceof ConfigIncluderFile
            ? ((ConfigIncluderFile)this.delegate).includeFile(context, what)
            : SimpleIncluder.includeFileWithoutFallback(context, what);
      }
   }

   private static class RelativeNameSource implements SimpleIncluder.NameSource {
      private final ConfigIncludeContext context;

      RelativeNameSource(ConfigIncludeContext context) {
         this.context = context;
      }

      @Override
      public ConfigParseable nameToParseable(String name, ConfigParseOptions options) {
         ConfigParseable p = this.context.relativeTo(name);
         return (ConfigParseable)(p == null ? Parseable.newNotFound(name, "include was not found: '" + name + "'", options) : p);
      }
   }
}
