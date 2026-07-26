package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldSavedData;

public class WorldSavedCubicChunksData extends WorldSavedData {
   public boolean isCubicChunks = false;
   public int minHeight = 0;
   public int maxHeight = 256;
   public ResourceLocation compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
   public ResourceLocation storageFormat = StorageFormatProviderBase.DEFAULT;

   public WorldSavedCubicChunksData(String name) {
      super(name);
   }

   public WorldSavedCubicChunksData(String name, boolean isCC, int minHeight, int maxHeight) {
      this(name);
      if (isCC) {
         this.minHeight = minHeight;
         this.maxHeight = maxHeight;
         this.isCubicChunks = true;
         this.compatibilityGeneratorType = new ResourceLocation(CubicChunksConfig.compatibilityGeneratorType);
         this.storageFormat = StorageFormatProviderBase.defaultStorageFormatProviderName(CubicChunksConfig.storageFormat);
      }
   }

   public void func_76184_a(NBTTagCompound nbt) {
      this.minHeight = nbt.func_74762_e("minHeight");
      this.maxHeight = nbt.func_74762_e("maxHeight");
      this.isCubicChunks = !nbt.func_74764_b("isCubicChunks") || nbt.func_74767_n("isCubicChunks");
      if (nbt.func_74764_b("compatibilityGeneratorType")) {
         this.compatibilityGeneratorType = new ResourceLocation(nbt.func_74779_i("compatibilityGeneratorType"));
      } else {
         this.compatibilityGeneratorType = VanillaCompatibilityGeneratorProviderBase.DEFAULT;
      }

      if (nbt.func_74764_b("storageFormat")) {
         this.storageFormat = new ResourceLocation(nbt.func_74779_i("storageFormat"));
      } else {
         this.storageFormat = StorageFormatProviderBase.DEFAULT;
      }
   }

   public NBTTagCompound func_189551_b(NBTTagCompound compound) {
      compound.func_74768_a("minHeight", this.minHeight);
      compound.func_74768_a("maxHeight", this.maxHeight);
      compound.func_74757_a("isCubicChunks", this.isCubicChunks);
      compound.func_74778_a("compatibilityGeneratorType", this.compatibilityGeneratorType.toString());
      compound.func_74778_a("storageFormat", this.storageFormat.toString());
      return compound;
   }
}
