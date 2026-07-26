package com.flowpowered.noise.model;

import com.flowpowered.noise.exception.NoModuleException;
import com.flowpowered.noise.module.Module;

public class Cylinder {
   private Module module;

   public Cylinder(Module mod) {
      this.module = mod;
   }

   public Module getModule() {
      return this.module;
   }

   public void setModule(Module mod) {
      if (mod == null) {
         throw new IllegalArgumentException("Mod cannot be null");
      } else {
         this.module = mod;
      }
   }

   public double getValue(double angle, double height) {
      if (this.module == null) {
         throw new NoModuleException();
      } else {
         double x = Math.cos(Math.toRadians(angle));
         double z = Math.sin(Math.toRadians(angle));
         return this.module.getValue(x, height, z);
      }
   }
}
