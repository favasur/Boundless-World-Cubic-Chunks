package com.flowpowered.noise.module.modifier;

import com.flowpowered.noise.exception.NoModuleException;
import com.flowpowered.noise.module.Module;

public class Abs extends Module {
   public Abs() {
      super(1);
   }

   @Override
   public int getSourceModuleCount() {
      return 1;
   }

   @Override
   public double getValue(double x, double y, double z) {
      if (this.sourceModule == null) {
         throw new NoModuleException();
      } else {
         return Math.abs(this.sourceModule[0].getValue(x, y, z));
      }
   }
}
