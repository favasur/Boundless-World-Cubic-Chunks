package io.github.opencubicchunks.cubicchunks.api.world.storage;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.IForgeRegistry.MissingFactory;

public abstract class StorageFormatProviderBase implements IForgeRegistryEntry<StorageFormatProviderBase> {
   public static final ResourceLocation DEFAULT = new ResourceLocation("cubicchunks", "anvil3d");
   public static IForgeRegistry<StorageFormatProviderBase> REGISTRY;
   public ResourceLocation registryName;
   public String unlocalizedName;

   public StorageFormatProviderBase() {
   }

   public static void init() {
      REGISTRY = new RegistryBuilder()
         .setType(StorageFormatProviderBase.class)
         .setIDRange(0, 256)
         .setName(new ResourceLocation("cubicchunks", "storage_format_provider_registry"))
         .addCallback(StorageFormatProviderBase.StorageFormatCallbacks.INSTANCE)
         .create();
   }

   public static ResourceLocation defaultStorageFormatProviderName(String fallback) {
      if (!fallback.isEmpty()) {
         return new ResourceLocation(fallback);
      } else {
         ResourceLocation[] providersThatCanBeDefault = REGISTRY.getValuesCollection()
            .stream()
            .filter(StorageFormatProviderBase::canBeDefault)
            .map(StorageFormatProviderBase::getRegistryName)
            .toArray(ResourceLocation[]::new);
         return providersThatCanBeDefault.length == 1 ? providersThatCanBeDefault[0] : DEFAULT;
      }
   }

   public ResourceLocation getRegistryName() {
      return this.registryName;
   }

   public StorageFormatProviderBase setRegistryName(ResourceLocation registryNameIn) {
      this.registryName = registryNameIn;
      return this;
   }

   public Class<StorageFormatProviderBase> getRegistryType() {
      return StorageFormatProviderBase.class;
   }

   public String getUnlocalizedName() {
      return this.unlocalizedName;
   }

   public StorageFormatProviderBase setUnlocalizedName(String nameIn) {
      this.unlocalizedName = nameIn;
      return this;
   }

   public abstract ICubicStorage provideStorage(World var1, Path var2) throws IOException;

   public boolean canBeDefault() {
      return false;
   }

   private static class StorageFormatCallbacks implements MissingFactory<StorageFormatProviderBase> {
      private static final StorageFormatProviderBase.StorageFormatCallbacks INSTANCE = new StorageFormatProviderBase.StorageFormatCallbacks();

      private StorageFormatCallbacks() {
      }

      public StorageFormatProviderBase createMissing(ResourceLocation key, boolean isNetwork) {
         return isNetwork ? new StorageFormatProviderBase.StorageFormatCallbacks.DummyStorageFormat().setRegistryName(key) : null;
      }

      private static class DummyStorageFormat extends StorageFormatProviderBase {
         private DummyStorageFormat() {
         }

         @Override
         public ICubicStorage provideStorage(World world, Path path) throws IOException {
            throw new IllegalStateException("attempted to initialize storage for world " + world + " using dummy storage format " + this.getRegistryName());
         }
      }
   }
}
