package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import javax.annotation.Nonnull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

public class CubicFeatureData extends WorldSavedData {
   private NBTTagCompound tagCompound = new NBTTagCompound();

   public CubicFeatureData(String name) {
      super(name);
   }

   public void func_76184_a(NBTTagCompound nbt) {
      this.tagCompound = nbt.func_74775_l("Features");
   }

   @Nonnull
   public NBTTagCompound func_189551_b(NBTTagCompound compound) {
      compound.func_74782_a("Features", this.tagCompound);
      return compound;
   }

   public void writeInstance(NBTTagCompound tag, int cubeX, int cubeY, int cubeZ) {
      this.tagCompound.func_74782_a(formatChunkCoords(cubeX, cubeY, cubeZ), tag);
   }

   public static String formatChunkCoords(int chunkX, int chunkY, int chunkZ) {
      return "[" + chunkX + "," + chunkY + "," + chunkZ + "]";
   }

   public NBTTagCompound getTagCompound() {
      return this.tagCompound;
   }
}
