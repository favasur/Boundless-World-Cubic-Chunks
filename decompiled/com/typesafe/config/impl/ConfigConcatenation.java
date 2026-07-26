package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class ConfigConcatenation extends AbstractConfigValue implements Unmergeable {
   private final List<AbstractConfigValue> pieces;

   ConfigConcatenation(ConfigOrigin origin, List<AbstractConfigValue> pieces) {
      super(origin);
      this.pieces = pieces;
      if (pieces.size() < 2) {
         throw new ConfigException.BugOrBroken("Created concatenation with less than 2 items: " + this);
      } else {
         boolean hadUnmergeable = false;

         for (AbstractConfigValue p : pieces) {
            if (p instanceof ConfigConcatenation) {
               throw new ConfigException.BugOrBroken("ConfigConcatenation should never be nested: " + this);
            }

            if (p instanceof Unmergeable) {
               hadUnmergeable = true;
            }
         }

         if (!hadUnmergeable) {
            throw new ConfigException.BugOrBroken("Created concatenation without an unmergeable in it: " + this);
         }
      }
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

   protected ConfigConcatenation newCopy(ConfigOrigin newOrigin) {
      return new ConfigConcatenation(newOrigin, this.pieces);
   }

   @Override
   protected boolean ignoresFallbacks() {
      return false;
   }

   @Override
   public Collection<ConfigConcatenation> unmergedValues() {
      return Collections.singleton(this);
   }

   private static void join(ArrayList<AbstractConfigValue> builder, AbstractConfigValue origRight) {
      AbstractConfigValue left = builder.get(builder.size() - 1);
      AbstractConfigValue right = origRight;
      if (left instanceof ConfigObject && origRight instanceof SimpleConfigList) {
         left = DefaultTransformer.transform(left, ConfigValueType.LIST);
      } else if (left instanceof SimpleConfigList && origRight instanceof ConfigObject) {
         right = DefaultTransformer.transform(origRight, ConfigValueType.LIST);
      }

      AbstractConfigValue joined = null;
      if (left instanceof ConfigObject && right instanceof ConfigObject) {
         joined = right.withFallback(left);
      } else if (left instanceof SimpleConfigList && right instanceof SimpleConfigList) {
         joined = ((SimpleConfigList)left).concatenate((SimpleConfigList)right);
      } else {
         if (left instanceof ConfigConcatenation || right instanceof ConfigConcatenation) {
            throw new ConfigException.BugOrBroken("unflattened ConfigConcatenation");
         }

         if (!(left instanceof Unmergeable) && !(right instanceof Unmergeable)) {
            String s1 = left.transformToString();
            String s2 = right.transformToString();
            if (s1 == null || s2 == null) {
               throw new ConfigException.WrongType(
                  left.origin(), "Cannot concatenate object or list with a non-object-or-list, " + left + " and " + right + " are not compatible"
               );
            }

            ConfigOrigin joinedOrigin = SimpleConfigOrigin.mergeOrigins(left.origin(), right.origin());
            joined = new ConfigString(joinedOrigin, s1 + s2);
         }
      }

      if (joined == null) {
         builder.add(right);
      } else {
         builder.remove(builder.size() - 1);
         builder.add(joined);
      }
   }

   static List<AbstractConfigValue> consolidate(List<AbstractConfigValue> pieces) {
      if (pieces.size() < 2) {
         return pieces;
      } else {
         List<AbstractConfigValue> flattened = new ArrayList<>(pieces.size());

         for (AbstractConfigValue v : pieces) {
            if (v instanceof ConfigConcatenation) {
               flattened.addAll(((ConfigConcatenation)v).pieces);
            } else {
               flattened.add(v);
            }
         }

         ArrayList<AbstractConfigValue> consolidated = new ArrayList<>(flattened.size());

         for (AbstractConfigValue vx : flattened) {
            if (consolidated.isEmpty()) {
               consolidated.add(vx);
            } else {
               join(consolidated, vx);
            }
         }

         return consolidated;
      }
   }

   static AbstractConfigValue concatenate(List<AbstractConfigValue> pieces) {
      List<AbstractConfigValue> consolidated = consolidate(pieces);
      if (consolidated.isEmpty()) {
         return null;
      } else if (consolidated.size() == 1) {
         return consolidated.get(0);
      } else {
         ConfigOrigin mergedOrigin = SimpleConfigOrigin.mergeOrigins(consolidated);
         return new ConfigConcatenation(mergedOrigin, consolidated);
      }
   }

   @Override
   AbstractConfigValue resolveSubstitutions(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      List<AbstractConfigValue> resolved = new ArrayList<>(this.pieces.size());

      for (AbstractConfigValue p : this.pieces) {
         AbstractConfigValue r = context.unrestricted().resolve(p);
         if (r != null) {
            resolved.add(r);
         }
      }

      List<AbstractConfigValue> joined = consolidate(resolved);
      if (joined.size() != 1) {
         throw new ConfigException.BugOrBroken("Resolved list should always join to exactly one value, not " + joined);
      } else {
         return joined.get(0);
      }
   }

   @Override
   ResolveStatus resolveStatus() {
      return ResolveStatus.UNRESOLVED;
   }

   ConfigConcatenation relativized(Path prefix) {
      List<AbstractConfigValue> newPieces = new ArrayList<>();

      for (AbstractConfigValue p : this.pieces) {
         newPieces.add(p.relativized(prefix));
      }

      return new ConfigConcatenation(this.origin(), newPieces);
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof ConfigConcatenation;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof ConfigConcatenation) ? false : this.canEqual(other) && this.pieces.equals(((ConfigConcatenation)other).pieces);
   }

   @Override
   public int hashCode() {
      return this.pieces.hashCode();
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      for (AbstractConfigValue p : this.pieces) {
         p.render(sb, indent, atRoot, options);
      }
   }

   static List<AbstractConfigValue> valuesFromPieces(ConfigOrigin origin, List<Object> pieces) {
      List<AbstractConfigValue> values = new ArrayList<>(pieces.size());

      for (Object p : pieces) {
         if (p instanceof SubstitutionExpression) {
            values.add(new ConfigReference(origin, (SubstitutionExpression)p));
         } else {
            if (!(p instanceof String)) {
               throw new ConfigException.BugOrBroken("Unexpected piece " + p);
            }

            values.add(new ConfigString(origin, (String)p));
         }
      }

      return values;
   }
}
