package org.spongepowered.asm.service.mojang;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;

public class MixinServiceLaunchWrapperBootstrap implements IMixinServiceBootstrap {
   private static final String SERVICE_PACKAGE = "org.spongepowered.asm.service.";
   private static final String MIXIN_UTIL_PACKAGE = "org.spongepowered.asm.util.";
   private static final String LEGACY_ASM_PACKAGE = "org.spongepowered.asm.lib.";
   private static final String ASM_PACKAGE = "org.objectweb.asm.";
   private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";

   public MixinServiceLaunchWrapperBootstrap() {
   }

   @Override
   public String getName() {
      return "LaunchWrapper";
   }

   @Override
   public String getServiceClassName() {
      return "org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper";
   }

   @Override
   public void bootstrap() {
      try {
         Launch.classLoader.hashCode();
      } catch (Throwable var2) {
         throw new ServiceInitialisationException(this.getName() + " is not available");
      }

      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.service.");
      Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.lib.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.mixin.");
      Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.util.");
   }
}
