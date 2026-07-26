package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class ConfigDelayedMerge extends AbstractConfigValue implements Unmergeable, ReplaceableMergeStack {
   private final List<AbstractConfigValue> stack;

   ConfigDelayedMerge(ConfigOrigin origin, List<AbstractConfigValue> stack) {
      super(origin);
      this.stack = stack;
      if (stack.isEmpty()) {
         throw new ConfigException.BugOrBroken("creating empty delayed merge value");
      } else {
         for (AbstractConfigValue v : stack) {
            if (v instanceof ConfigDelayedMerge || v instanceof ConfigDelayedMergeObject) {
               throw new ConfigException.BugOrBroken("placed nested DelayedMerge in a ConfigDelayedMerge, should have consolidated stack");
            }
         }
      }
   }

   @Override
   public ConfigValueType valueType() {
      throw new ConfigException.NotResolved("called valueType() on value with unresolved substitutions, need to Config#resolve() first, see API docs");
   }

   @Override
   public Object unwrapped() {
      throw new ConfigException.NotResolved("called unwrapped() on value with unresolved substitutions, need to Config#resolve() first, see API docs");
   }

   @Override
   AbstractConfigValue resolveSubstitutions(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      return resolveSubstitutions(this, this.stack, context);
   }

   static AbstractConfigValue resolveSubstitutions(ReplaceableMergeStack replaceable, List<AbstractConfigValue> stack, ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      int count = 0;
      AbstractConfigValue merged = null;

      for (AbstractConfigValue v : stack) {
         if (v instanceof ReplaceableMergeStack) {
            throw new ConfigException.BugOrBroken("A delayed merge should not contain another one: " + replaceable);
         }

         boolean replaced = false;
         if (v instanceof Unmergeable) {
            context.source().replace((AbstractConfigValue)replaceable, replaceable.makeReplacer(count + 1));
            replaced = true;
         }

         AbstractConfigValue resolved;
         try {
            resolved = context.resolve(v);
         } finally {
            if (replaced) {
               context.source().unreplace((AbstractConfigValue)replaceable);
            }
         }

         if (resolved != null) {
            if (merged == null) {
               merged = resolved;
            } else {
               merged = merged.withFallback(resolved);
            }
         }

         count++;
      }

      return merged;
   }

   @Override
   public ResolveReplacer makeReplacer(final int skipping) {
      return new ResolveReplacer() {
         @Override
         protected AbstractConfigValue makeReplacement(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
            return ConfigDelayedMerge.makeReplacement(context, ConfigDelayedMerge.this.stack, skipping);
         }
      };
   }

   static AbstractConfigValue makeReplacement(ResolveContext context, List<AbstractConfigValue> stack, int skipping) throws AbstractConfigValue.NotPossibleToResolve {
      List<AbstractConfigValue> subStack = stack.subList(skipping, stack.size());
      if (subStack.isEmpty()) {
         throw new AbstractConfigValue.NotPossibleToResolve(context);
      } else {
         AbstractConfigValue merged = null;

         for (AbstractConfigValue v : subStack) {
            if (merged == null) {
               merged = v;
            } else {
               merged = merged.withFallback(v);
            }
         }

         return merged;
      }
   }

   @Override
   ResolveStatus resolveStatus() {
      return ResolveStatus.UNRESOLVED;
   }

   ConfigDelayedMerge relativized(Path prefix) {
      List<AbstractConfigValue> newStack = new ArrayList<>();

      for (AbstractConfigValue o : this.stack) {
         newStack.add(o.relativized(prefix));
      }

      return new ConfigDelayedMerge(this.origin(), newStack);
   }

   static boolean stackIgnoresFallbacks(List<AbstractConfigValue> stack) {
      AbstractConfigValue last = stack.get(stack.size() - 1);
      return last.ignoresFallbacks();
   }

   @Override
   protected boolean ignoresFallbacks() {
      return stackIgnoresFallbacks(this.stack);
   }

   @Override
   protected AbstractConfigValue newCopy(ConfigOrigin newOrigin) {
      return new ConfigDelayedMerge(newOrigin, this.stack);
   }

   protected final ConfigDelayedMerge mergedWithTheUnmergeable(Unmergeable fallback) {
      return (ConfigDelayedMerge)this.mergedWithTheUnmergeable(this.stack, fallback);
   }

   protected final ConfigDelayedMerge mergedWithObject(AbstractConfigObject fallback) {
      return (ConfigDelayedMerge)this.mergedWithObject(this.stack, fallback);
   }

   protected ConfigDelayedMerge mergedWithNonObject(AbstractConfigValue fallback) {
      return (ConfigDelayedMerge)this.mergedWithNonObject(this.stack, fallback);
   }

   @Override
   public Collection<AbstractConfigValue> unmergedValues() {
      return this.stack;
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof ConfigDelayedMerge;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof ConfigDelayedMerge) ? false : this.canEqual(other) && this.stack.equals(((ConfigDelayedMerge)other).stack);
   }

   @Override
   public int hashCode() {
      return this.stack.hashCode();
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, String atKey, ConfigRenderOptions options) {
      render(this.stack, sb, indent, atRoot, atKey, options);
   }

   static void render(List<AbstractConfigValue> stack, StringBuilder sb, int indent, boolean atRoot, String atKey, ConfigRenderOptions options) {
      boolean commentMerge = options.getComments();
      if (commentMerge) {
         sb.append("# unresolved merge of " + stack.size() + " values follows (\n");
         if (atKey == null) {
            indent(sb, indent, options);
            sb.append("# this unresolved merge will not be parseable because it's at the root of the object\n");
            indent(sb, indent, options);
            sb.append("# the HOCON format has no way to list multiple root objects in a single file\n");
         }
      }

      List<AbstractConfigValue> reversed = new ArrayList<>();
      reversed.addAll(stack);
      Collections.reverse(reversed);
      int i = 0;

      for (AbstractConfigValue v : reversed) {
         if (commentMerge) {
            indent(sb, indent, options);
            if (atKey != null) {
               sb.append("#     unmerged value " + i + " for key " + ConfigImplUtil.renderJsonString(atKey) + " from ");
            } else {
               sb.append("#     unmerged value " + i + " from ");
            }

            i++;
            sb.append(v.origin().description());
            sb.append("\n");

            for (String comment : v.origin().comments()) {
               indent(sb, indent, options);
               sb.append("# ");
               sb.append(comment);
               sb.append("\n");
            }
         }

         indent(sb, indent, options);
         if (atKey != null) {
            sb.append(ConfigImplUtil.renderJsonString(atKey));
            if (options.getFormatted()) {
               sb.append(" : ");
            } else {
               sb.append(":");
            }
         }

         v.render(sb, indent, atRoot, options);
         sb.append(",");
         if (options.getFormatted()) {
            sb.append('\n');
         }
      }

      sb.setLength(sb.length() - 1);
      if (options.getFormatted()) {
         sb.setLength(sb.length() - 1);
         sb.append("\n");
      }

      if (commentMerge) {
         indent(sb, indent, options);
         sb.append("# ) end of unresolved merge\n");
      }
   }
}
