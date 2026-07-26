package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class ConfigDelayedMergeObject extends AbstractConfigObject implements Unmergeable, ReplaceableMergeStack {
   private final List<AbstractConfigValue> stack;

   ConfigDelayedMergeObject(ConfigOrigin origin, List<AbstractConfigValue> stack) {
      super(origin);
      this.stack = stack;
      if (stack.isEmpty()) {
         throw new ConfigException.BugOrBroken("creating empty delayed merge object");
      } else if (!(stack.get(0) instanceof AbstractConfigObject)) {
         throw new ConfigException.BugOrBroken("created a delayed merge object not guaranteed to be an object");
      } else {
         for (AbstractConfigValue v : stack) {
            if (v instanceof ConfigDelayedMerge || v instanceof ConfigDelayedMergeObject) {
               throw new ConfigException.BugOrBroken("placed nested DelayedMerge in a ConfigDelayedMergeObject, should have consolidated stack");
            }
         }
      }
   }

   protected ConfigDelayedMergeObject newCopy(ResolveStatus status, ConfigOrigin origin) {
      if (status != this.resolveStatus()) {
         throw new ConfigException.BugOrBroken("attempt to create resolved ConfigDelayedMergeObject");
      } else {
         return new ConfigDelayedMergeObject(origin, this.stack);
      }
   }

   @Override
   AbstractConfigObject resolveSubstitutions(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      AbstractConfigValue merged = ConfigDelayedMerge.resolveSubstitutions(this, this.stack, context);
      if (merged instanceof AbstractConfigObject) {
         return (AbstractConfigObject)merged;
      } else {
         throw new ConfigException.BugOrBroken("somehow brokenly merged an object and didn't get an object, got " + merged);
      }
   }

   @Override
   public ResolveReplacer makeReplacer(final int skipping) {
      return new ResolveReplacer() {
         @Override
         protected AbstractConfigValue makeReplacement(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
            return ConfigDelayedMerge.makeReplacement(context, ConfigDelayedMergeObject.this.stack, skipping);
         }
      };
   }

   @Override
   ResolveStatus resolveStatus() {
      return ResolveStatus.UNRESOLVED;
   }

   ConfigDelayedMergeObject relativized(Path prefix) {
      List<AbstractConfigValue> newStack = new ArrayList<>();

      for (AbstractConfigValue o : this.stack) {
         newStack.add(o.relativized(prefix));
      }

      return new ConfigDelayedMergeObject(this.origin(), newStack);
   }

   @Override
   protected boolean ignoresFallbacks() {
      return ConfigDelayedMerge.stackIgnoresFallbacks(this.stack);
   }

   protected final ConfigDelayedMergeObject mergedWithTheUnmergeable(Unmergeable fallback) {
      this.requireNotIgnoringFallbacks();
      return (ConfigDelayedMergeObject)this.mergedWithTheUnmergeable(this.stack, fallback);
   }

   protected final ConfigDelayedMergeObject mergedWithObject(AbstractConfigObject fallback) {
      return this.mergedWithNonObject(fallback);
   }

   protected final ConfigDelayedMergeObject mergedWithNonObject(AbstractConfigValue fallback) {
      this.requireNotIgnoringFallbacks();
      return (ConfigDelayedMergeObject)this.mergedWithNonObject(this.stack, fallback);
   }

   public ConfigDelayedMergeObject withFallback(ConfigMergeable mergeable) {
      return (ConfigDelayedMergeObject)super.withFallback(mergeable);
   }

   public ConfigDelayedMergeObject withOnlyKey(String key) {
      throw notResolved();
   }

   public ConfigDelayedMergeObject withoutKey(String key) {
      throw notResolved();
   }

   @Override
   protected AbstractConfigObject withOnlyPathOrNull(Path path) {
      throw notResolved();
   }

   @Override
   AbstractConfigObject withOnlyPath(Path path) {
      throw notResolved();
   }

   @Override
   AbstractConfigObject withoutPath(Path path) {
      throw notResolved();
   }

   public ConfigDelayedMergeObject withValue(String key, ConfigValue value) {
      throw notResolved();
   }

   ConfigDelayedMergeObject withValue(Path path, ConfigValue value) {
      throw notResolved();
   }

   @Override
   public Collection<AbstractConfigValue> unmergedValues() {
      return this.stack;
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof ConfigDelayedMergeObject;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof ConfigDelayedMergeObject) ? false : this.canEqual(other) && this.stack.equals(((ConfigDelayedMergeObject)other).stack);
   }

   @Override
   public int hashCode() {
      return this.stack.hashCode();
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, String atKey, ConfigRenderOptions options) {
      ConfigDelayedMerge.render(this.stack, sb, indent, atRoot, atKey, options);
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      this.render(sb, indent, atRoot, null, options);
   }

   private static ConfigException notResolved() {
      return new ConfigException.NotResolved("need to Config#resolve() before using this object, see the API docs for Config#resolve()");
   }

   @Override
   public Map<String, Object> unwrapped() {
      throw notResolved();
   }

   @Override
   public AbstractConfigValue get(Object key) {
      throw notResolved();
   }

   @Override
   public boolean containsKey(Object key) {
      throw notResolved();
   }

   @Override
   public boolean containsValue(Object value) {
      throw notResolved();
   }

   @Override
   public Set<Entry<String, ConfigValue>> entrySet() {
      throw notResolved();
   }

   @Override
   public boolean isEmpty() {
      throw notResolved();
   }

   @Override
   public Set<String> keySet() {
      throw notResolved();
   }

   @Override
   public int size() {
      throw notResolved();
   }

   @Override
   public Collection<ConfigValue> values() {
      throw notResolved();
   }

   @Override
   protected AbstractConfigValue attemptPeekWithPartialResolve(String key) {
      for (AbstractConfigValue layer : this.stack) {
         if (!(layer instanceof AbstractConfigObject)) {
            if (layer instanceof Unmergeable) {
               throw new ConfigException.NotResolved(
                  "Key '"
                     + key
                     + "' is not available at '"
                     + this.origin().description()
                     + "' because value at '"
                     + layer.origin().description()
                     + "' has not been resolved and may turn out to contain or hide '"
                     + key
                     + "'."
                     + " Be sure to Config#resolve() before using a config object."
               );
            }

            if (layer.resolveStatus() == ResolveStatus.UNRESOLVED) {
               if (!(layer instanceof ConfigList)) {
                  throw new ConfigException.BugOrBroken("Expecting a list here, not " + layer);
               }

               return null;
            }

            if (!layer.ignoresFallbacks()) {
               throw new ConfigException.BugOrBroken("resolved non-object should ignore fallbacks");
            }

            return null;
         }

         AbstractConfigObject objectLayer = (AbstractConfigObject)layer;
         AbstractConfigValue v = objectLayer.attemptPeekWithPartialResolve(key);
         if (v != null) {
            if (v.ignoresFallbacks()) {
               return v;
            }
         } else if (layer instanceof Unmergeable) {
            throw new ConfigException.BugOrBroken("should not be reached: unmergeable object returned null value");
         }
      }

      throw new ConfigException.BugOrBroken("Delayed merge stack does not contain any unmergeable values");
   }
}
