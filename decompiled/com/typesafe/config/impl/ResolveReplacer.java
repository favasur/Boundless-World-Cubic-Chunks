package com.typesafe.config.impl;

abstract class ResolveReplacer {
   private volatile AbstractConfigValue replacement = null;
   static final ResolveReplacer cycleResolveReplacer = new ResolveReplacer() {
      @Override
      protected AbstractConfigValue makeReplacement(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
         throw new AbstractConfigValue.NotPossibleToResolve(context);
      }
   };

   ResolveReplacer() {
   }

   final AbstractConfigValue replace(ResolveContext context) throws AbstractConfigValue.NotPossibleToResolve {
      if (this.replacement == null) {
         this.replacement = this.makeReplacement(context);
      }

      return this.replacement;
   }

   protected abstract AbstractConfigValue makeReplacement(ResolveContext var1) throws AbstractConfigValue.NotPossibleToResolve;
}
