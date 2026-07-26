package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigResolveOptions;
import java.util.ArrayList;
import java.util.List;

final class ResolveContext {
   private final ResolveSource source;
   private final ResolveMemos memos;
   private final ConfigResolveOptions options;
   private final Path restrictToChild;
   private final List<SubstitutionExpression> expressionTrace;

   ResolveContext(ResolveSource source, ResolveMemos memos, ConfigResolveOptions options, Path restrictToChild, List<SubstitutionExpression> expressionTrace) {
      this.source = source;
      this.memos = memos;
      this.options = options;
      this.restrictToChild = restrictToChild;
      this.expressionTrace = expressionTrace;
   }

   ResolveContext(AbstractConfigObject root, ConfigResolveOptions options, Path restrictToChild) {
      this(new ResolveSource(root), new ResolveMemos(), options, restrictToChild, new ArrayList<>());
   }

   ResolveSource source() {
      return this.source;
   }

   ConfigResolveOptions options() {
      return this.options;
   }

   boolean isRestrictedToChild() {
      return this.restrictToChild != null;
   }

   Path restrictToChild() {
      return this.restrictToChild;
   }

   ResolveContext restrict(Path restrictTo) {
      return restrictTo == this.restrictToChild ? this : new ResolveContext(this.source, this.memos, this.options, restrictTo, this.expressionTrace);
   }

   ResolveContext unrestricted() {
      return this.restrict(null);
   }

   void trace(SubstitutionExpression expr) {
      this.expressionTrace.add(expr);
   }

   void untrace() {
      this.expressionTrace.remove(this.expressionTrace.size() - 1);
   }

   String traceString() {
      String separator = ", ";
      StringBuilder sb = new StringBuilder();

      for (SubstitutionExpression expr : this.expressionTrace) {
         sb.append(expr.toString());
         sb.append(separator);
      }

      if (sb.length() > 0) {
         sb.setLength(sb.length() - separator.length());
      }

      return sb.toString();
   }

   AbstractConfigValue resolve(AbstractConfigValue original) throws AbstractConfigValue.NotPossibleToResolve {
      MemoKey fullKey = new MemoKey(original, null);
      MemoKey restrictedKey = null;
      AbstractConfigValue cached = this.memos.get(fullKey);
      if (cached == null && this.isRestrictedToChild()) {
         restrictedKey = new MemoKey(original, this.restrictToChild());
         cached = this.memos.get(restrictedKey);
      }

      if (cached != null) {
         return cached;
      } else {
         AbstractConfigValue resolved = this.source.resolveCheckingReplacement(this, original);
         if (resolved == null || resolved.resolveStatus() == ResolveStatus.RESOLVED) {
            this.memos.put(fullKey, resolved);
         } else if (this.isRestrictedToChild()) {
            if (restrictedKey == null) {
               throw new ConfigException.BugOrBroken("restrictedKey should not be null here");
            }

            this.memos.put(restrictedKey, resolved);
         } else {
            if (!this.options().getAllowUnresolved()) {
               throw new ConfigException.BugOrBroken("resolveSubstitutions() did not give us a resolved object");
            }

            this.memos.put(fullKey, resolved);
         }

         return resolved;
      }
   }

   static AbstractConfigValue resolve(AbstractConfigValue value, AbstractConfigObject root, ConfigResolveOptions options) {
      ResolveContext context = new ResolveContext(root, options, null);

      try {
         return context.resolve(value);
      } catch (AbstractConfigValue.NotPossibleToResolve var5) {
         throw new ConfigException.BugOrBroken("NotPossibleToResolve was thrown from an outermost resolve", var5);
      }
   }
}
