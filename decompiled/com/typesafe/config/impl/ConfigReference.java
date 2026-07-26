package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueType;
import java.util.Collection;
import java.util.Collections;

final class ConfigReference extends AbstractConfigValue implements Unmergeable {
   private final SubstitutionExpression expr;
   private final int prefixLength;

   ConfigReference(ConfigOrigin origin, SubstitutionExpression expr) {
      this(origin, expr, 0);
   }

   private ConfigReference(ConfigOrigin origin, SubstitutionExpression expr, int prefixLength) {
      super(origin);
      this.expr = expr;
      this.prefixLength = prefixLength;
   }

   private ConfigException.NotResolved notResolved() {
      return new ConfigException.NotResolved("need to Config#resolve(), see the API docs for Config#resolve(); substitution not resolved: " + this);
   }

   @Override
   public ConfigValueType valueType() {
      throw this.notResolved();
   }

   @Override
   public Object unwrapped() {
      throw this.notResolved();
   }

   protected ConfigReference newCopy(ConfigOrigin newOrigin) {
      return new ConfigReference(newOrigin, this.expr, this.prefixLength);
   }

   @Override
   protected boolean ignoresFallbacks() {
      return false;
   }

   @Override
   public Collection<ConfigReference> unmergedValues() {
      return Collections.singleton(this);
   }

   @Override
   AbstractConfigValue resolveSubstitutions(ResolveContext context) {
      context.source().replace(this, ResolveReplacer.cycleResolveReplacer);

      ConfigReference e;
      try {
         AbstractConfigValue v;
         try {
            v = context.source().lookupSubst(context, this.expr, this.prefixLength);
         } catch (AbstractConfigValue.NotPossibleToResolve var7) {
            if (!this.expr.optional()) {
               throw new ConfigException.UnresolvedSubstitution(
                  this.origin(), this.expr + " was part of a cycle of substitutions involving " + var7.traceString(), var7
               );
            }

            v = null;
         }

         if (v != null || this.expr.optional()) {
            return v;
         }

         if (!context.options().getAllowUnresolved()) {
            throw new ConfigException.UnresolvedSubstitution(this.origin(), this.expr.toString());
         }

         e = this;
      } finally {
         context.source().unreplace(this);
      }

      return e;
   }

   @Override
   ResolveStatus resolveStatus() {
      return ResolveStatus.UNRESOLVED;
   }

   ConfigReference relativized(Path prefix) {
      SubstitutionExpression newExpr = this.expr.changePath(this.expr.path().prepend(prefix));
      return new ConfigReference(this.origin(), newExpr, this.prefixLength + prefix.length());
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof ConfigReference;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof ConfigReference) ? false : this.canEqual(other) && this.expr.equals(((ConfigReference)other).expr);
   }

   @Override
   public int hashCode() {
      return this.expr.hashCode();
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      sb.append(this.expr.toString());
   }

   SubstitutionExpression expression() {
      return this.expr;
   }
}
