package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class AbstractConfigObject extends AbstractConfigValue implements ConfigObject {
   private final SimpleConfig config = new SimpleConfig(this);

   protected AbstractConfigObject(ConfigOrigin origin) {
      super(origin);
   }

   public SimpleConfig toConfig() {
      return this.config;
   }

   public AbstractConfigObject toFallbackValue() {
      return this;
   }

   public abstract AbstractConfigObject withOnlyKey(String var1);

   public abstract AbstractConfigObject withoutKey(String var1);

   public abstract AbstractConfigObject withValue(String var1, ConfigValue var2);

   protected abstract AbstractConfigObject withOnlyPathOrNull(Path var1);

   abstract AbstractConfigObject withOnlyPath(Path var1);

   abstract AbstractConfigObject withoutPath(Path var1);

   abstract AbstractConfigObject withValue(Path var1, ConfigValue var2);

   protected final AbstractConfigValue peekAssumingResolved(String key, Path originalPath) {
      try {
         return this.attemptPeekWithPartialResolve(key);
      } catch (ConfigException.NotResolved var4) {
         throw ConfigImpl.improveNotResolved(originalPath, var4);
      }
   }

   protected abstract AbstractConfigValue attemptPeekWithPartialResolve(String var1);

   protected AbstractConfigValue peekPath(Path path, ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      return peekPath(this, path, context);
   }

   AbstractConfigValue peekPath(Path path) {
      try {
         return peekPath(this, path, null);
      } catch (AbstractConfigValue.NotPossibleToResolve var3) {
         throw new ConfigException.BugOrBroken("NotPossibleToResolve happened though we had no ResolveContext in peekPath");
      }
   }

   private static AbstractConfigValue peekPath(AbstractConfigObject self, Path path, ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      try {
         if (context != null) {
            AbstractConfigValue partiallyResolved = context.restrict(path).resolve(self);
            if (partiallyResolved instanceof AbstractConfigObject) {
               return peekPath((AbstractConfigObject)partiallyResolved, path, null);
            } else {
               throw new ConfigException.BugOrBroken("resolved object to non-object " + self + " to " + partiallyResolved);
            }
         } else {
            Path next = path.remainder();
            AbstractConfigValue v = self.attemptPeekWithPartialResolve(path.first());
            if (next == null) {
               return v;
            } else {
               return v instanceof AbstractConfigObject ? peekPath((AbstractConfigObject)v, next, null) : null;
            }
         }
      } catch (ConfigException.NotResolved var5) {
         throw ConfigImpl.improveNotResolved(path, var5);
      }
   }

   @Override
   public ConfigValueType valueType() {
      return ConfigValueType.OBJECT;
   }

   protected abstract AbstractConfigObject newCopy(ResolveStatus var1, ConfigOrigin var2);

   protected AbstractConfigObject newCopy(ConfigOrigin origin) {
      return this.newCopy(this.resolveStatus(), origin);
   }

   protected AbstractConfigObject constructDelayedMerge(ConfigOrigin origin, List<AbstractConfigValue> stack) {
      return new ConfigDelayedMergeObject(origin, stack);
   }

   protected abstract AbstractConfigObject mergedWithObject(AbstractConfigObject var1);

   public AbstractConfigObject withFallback(ConfigMergeable mergeable) {
      return (AbstractConfigObject)super.withFallback(mergeable);
   }

   static ConfigOrigin mergeOrigins(Collection<? extends AbstractConfigValue> stack) {
      if (stack.isEmpty()) {
         throw new ConfigException.BugOrBroken("can't merge origins on empty list");
      } else {
         List<ConfigOrigin> origins = new ArrayList<>();
         ConfigOrigin firstOrigin = null;
         int numMerged = 0;

         for (AbstractConfigValue v : stack) {
            if (firstOrigin == null) {
               firstOrigin = v.origin();
            }

            if (!(v instanceof AbstractConfigObject) || ((AbstractConfigObject)v).resolveStatus() != ResolveStatus.RESOLVED || !((ConfigObject)v).isEmpty()) {
               origins.add(v.origin());
               numMerged++;
            }
         }

         if (numMerged == 0) {
            origins.add(firstOrigin);
         }

         return SimpleConfigOrigin.mergeOrigins(origins);
      }
   }

   static ConfigOrigin mergeOrigins(AbstractConfigObject... stack) {
      return mergeOrigins(Arrays.asList(stack));
   }

   abstract AbstractConfigObject resolveSubstitutions(ResolveContext var1) throws AbstractConfigValue.NotPossibleToResolve;

   abstract AbstractConfigObject relativized(Path var1);

   public abstract AbstractConfigValue get(Object var1);

   @Override
   protected abstract void render(StringBuilder var1, int var2, boolean var3, ConfigRenderOptions var4);

   private static UnsupportedOperationException weAreImmutable(String method) {
      return new UnsupportedOperationException("ConfigObject is immutable, you can't call Map." + method);
   }

   @Override
   public void clear() {
      throw weAreImmutable("clear");
   }

   public ConfigValue put(String arg0, ConfigValue arg1) {
      throw weAreImmutable("put");
   }

   @Override
   public void putAll(Map<? extends String, ? extends ConfigValue> arg0) {
      throw weAreImmutable("putAll");
   }

   public ConfigValue remove(Object arg0) {
      throw weAreImmutable("remove");
   }
}
