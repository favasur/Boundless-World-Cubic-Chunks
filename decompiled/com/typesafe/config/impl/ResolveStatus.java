package com.typesafe.config.impl;

import java.util.Collection;

enum ResolveStatus {
   UNRESOLVED,
   RESOLVED;

   private ResolveStatus() {
   }

   static final ResolveStatus fromValues(Collection<? extends AbstractConfigValue> values) {
      for (AbstractConfigValue v : values) {
         if (v.resolveStatus() == UNRESOLVED) {
            return UNRESOLVED;
         }
      }

      return RESOLVED;
   }

   static final ResolveStatus fromBoolean(boolean resolved) {
      return resolved ? RESOLVED : UNRESOLVED;
   }
}
