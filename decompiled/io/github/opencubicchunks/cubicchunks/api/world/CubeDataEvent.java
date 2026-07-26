package io.github.opencubicchunks.cubicchunks.api.world;

import net.minecraft.nbt.NBTTagCompound;

public class CubeDataEvent extends CubeEvent {
   private final NBTTagCompound data;

   public CubeDataEvent(ICube cube, NBTTagCompound data) {
      super(cube);
      this.data = data;
   }

   public NBTTagCompound getData() {
      return this.data;
   }

   public static class Load extends CubeDataEvent {
      public Load(ICube cube, NBTTagCompound data) {
         super(cube, data);
      }
   }

   public static class Save extends CubeDataEvent {
      public Save(ICube cube, NBTTagCompound data) {
         super(cube, data);
      }
   }
}
