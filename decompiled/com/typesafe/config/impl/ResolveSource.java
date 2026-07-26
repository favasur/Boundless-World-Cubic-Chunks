package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import java.util.IdentityHashMap;
import java.util.Map;

final class ResolveSource {
   private final AbstractConfigObject root;
   private final Map<AbstractConfigValue, ResolveReplacer> replacements;

   ResolveSource(AbstractConfigObject root) {
      this.root = root;
      this.replacements = new IdentityHashMap<>();
   }

   private static AbstractConfigValue findInObject(AbstractConfigObject obj, ResolveContext context, SubstitutionExpression subst) throws AbstractConfigValue.NotPossibleToResolve {
      return obj.peekPath(subst.path(), context);
   }

   AbstractConfigValue lookupSubst(ResolveContext context, SubstitutionExpression subst, int prefixLength) throws AbstractConfigValue.NotPossibleToResolve {
      context.trace(subst);

      AbstractConfigValue var9;
      try {
         AbstractConfigValue result = findInObject(this.root, context, subst);
         if (result == null) {
            SubstitutionExpression unprefixed = subst.changePath(subst.path().subPath(prefixLength));
            context.untrace();
            context.trace(unprefixed);
            if (prefixLength > 0) {
               result = findInObject(this.root, context, unprefixed);
            }

            if (result == null && context.options().getUseSystemEnvironment()) {
               result = findInObject(ConfigImpl.envVariablesAsConfigObject(), context, unprefixed);
            }
         }

         if (result != null) {
            result = context.resolve(result);
         }

         var9 = result;
      } finally {
         context.untrace();
      }

      return var9;
   }

   void replace(AbstractConfigValue value, ResolveReplacer replacer) {
      ResolveReplacer old = this.replacements.put(value, replacer);
      if (old != null) {
         throw new ConfigException.BugOrBroken("should not have replaced the same value twice: " + value);
      }
   }

   void unreplace(AbstractConfigValue value) {
      ResolveReplacer replacer = this.replacements.remove(value);
      if (replacer == null) {
         throw new ConfigException.BugOrBroken("unreplace() without replace(): " + value);
      }
   }

   private AbstractConfigValue replacement(ResolveContext context, AbstractConfigValue value) throws AbstractConfigValue.NotPossibleToResolve {
      ResolveReplacer replacer = this.replacements.get(value);
      return replacer == null ? value : replacer.replace(context);
   }

   AbstractConfigValue resolveCheckingReplacement(ResolveContext context, AbstractConfigValue original) throws AbstractConfigValue.NotPossibleToResolve {
      AbstractConfigValue replacement = this.replacement(context, original);
      return replacement != original ? context.resolve(replacement) : original.resolveSubstitutions(context);
   }
}
