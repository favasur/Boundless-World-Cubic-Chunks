package com.flowpowered.noise.module.combiner;

import com.flowpowered.noise.exception.NoModuleException;
import com.flowpowered.noise.module.Module;

public class Add extends Module {
   public Add() {
      super(2);
   }

   @Override
   public int getSourceModuleCount() {
      return 2;
   }

   @Override
   public double getValue(double x, double y, double z) {
      if (this.sourceModule[0] == null) {
         throw new NoModuleException();
      } else if (this.sourceModule[1] == null) {
         throw new NoModuleException();
      } else {
         return this.sourceModule[0].getValue(x, y, z) + this.sourceModule[1].getValue(x, y, z);
      }
   }
}
