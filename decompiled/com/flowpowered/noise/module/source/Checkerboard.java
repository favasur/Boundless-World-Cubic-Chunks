package com.flowpowered.noise.module.source;

import com.flowpowered.noise.Utils;
import com.flowpowered.noise.module.Module;

public class Checkerboard extends Module {
   public Checkerboard() {
      super(0);
   }

   @Override
   public int getSourceModuleCount() {
      return 0;
   }

   @Override
   public double getValue(double x, double y, double z) {
      int ix = Utils.floor(Utils.makeInt32Range(x));
      int iy = Utils.floor(Utils.makeInt32Range(y));
      int iz = Utils.floor(Utils.makeInt32Range(z));
      return (ix & 1 ^ iy & 1 ^ iz & 1) != 0 ? 0.0 : 1.0;
   }
}
