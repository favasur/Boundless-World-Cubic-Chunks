package io.github.opencubicchunks.cubicchunks.api.worldgen;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.IForgeRegistry.MissingFactory;

public abstract class VanillaCompatibilityGeneratorProviderBase implements IForgeRegistryEntry<VanillaCompatibilityGeneratorProviderBase> {
   public static final ResourceLocation DEFAULT = new ResourceLocation("cubicchunks", "default");
   public static IForgeRegistry<VanillaCompatibilityGeneratorProviderBase> REGISTRY;
   public ResourceLocation registryName;
   public String unlocalizedName;

   public VanillaCompatibilityGeneratorProviderBase() {
   }

   public static void init() {
      REGISTRY = new RegistryBuilder()
         .setType(VanillaCompatibilityGeneratorProviderBase.class)
         .setIDRange(0, 256)
         .setName(new ResourceLocation("cubicchunks", "vanilla_compatibility_generators_registry"))
         .setDefaultKey(DEFAULT)
         .addCallback(VanillaCompatibilityGeneratorProviderBase.VanillaCompatibilityGeneratorCallbacks.INSTANCE)
         .create();
   }

   public VanillaCompatibilityGeneratorProviderBase setRegistryName(ResourceLocation registryNameIn) {
      this.registryName = registryNameIn;
      return this;
   }

   public ResourceLocation getRegistryName() {
      return this.registryName;
   }

   public Class<VanillaCompatibilityGeneratorProviderBase> getRegistryType() {
      return VanillaCompatibilityGeneratorProviderBase.class;
   }

   public VanillaCompatibilityGeneratorProviderBase setUnlocalizedName(String nameIn) {
      this.unlocalizedName = nameIn;
      return this;
   }

   public String getUnlocalizedName() {
      return this.unlocalizedName;
   }

   public abstract ICubeGenerator provideGenerator(IChunkGenerator var1, World var2);

   private static class VanillaCompatibilityGeneratorCallbacks implements MissingFactory<VanillaCompatibilityGeneratorProviderBase> {
      private static final VanillaCompatibilityGeneratorProviderBase.VanillaCompatibilityGeneratorCallbacks INSTANCE = new VanillaCompatibilityGeneratorProviderBase.VanillaCompatibilityGeneratorCallbacks();

      private VanillaCompatibilityGeneratorCallbacks() {
      }

      public VanillaCompatibilityGeneratorProviderBase createMissing(ResourceLocation key, boolean isNetwork) {
         return isNetwork
            ? new VanillaCompatibilityGeneratorProviderBase.VanillaCompatibilityGeneratorCallbacks.DummyVanillaCompatibilityGenerator().setRegistryName(key)
            : null;
      }

      private static class DummyVanillaCompatibilityGenerator extends VanillaCompatibilityGeneratorProviderBase {
         private DummyVanillaCompatibilityGenerator() {
         }

         @Override
         public ICubeGenerator provideGenerator(IChunkGenerator vanillaChunkGenerator, World world) {
            throw new IllegalStateException(
               "attempted to initialize generator for world " + world + " using dummy vanilla compatibility generator " + this.getRegistryName()
            );
         }
      }
   }
}
