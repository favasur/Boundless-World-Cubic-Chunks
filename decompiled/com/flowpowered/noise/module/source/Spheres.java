package com.flowpowered.noise.module.source;

import com.flowpowered.noise.Utils;
import com.flowpowered.noise.module.Module;

public class Spheres extends Module {
   public static final double DEFAULT_SPHERES_FREQUENCY = 1.0;
   private double frequency = 1.0;

   public Spheres() {
      super(0);
   }

   public double getFrequency() {
      return this.frequency;
   }

   public void setFrequency(double frequency) {
      this.frequency = frequency;
   }

   @Override
   public int getSourceModuleCount() {
      return 0;
   }

   @Override
   public double getValue(double x, double y, double z) {
      double x1 = x * this.frequency;
      double y1 = y * this.frequency;
      double z1 = z * this.frequency;
      double distFromCenter = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
      double distFromSmallerSphere = distFromCenter - (double)Utils.floor(distFromCenter);
      double distFromLargerSphere = 1.0 - distFromSmallerSphere;
      double nearestDist = Math.min(distFromSmallerSphere, distFromLargerSphere);
      return 1.0 - nearestDist * 2.0;
   }
}
