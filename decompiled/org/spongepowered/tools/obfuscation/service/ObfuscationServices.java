package org.spongepowered.tools.obfuscation.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;

public final class ObfuscationServices {
   private static ObfuscationServices instance;
   private final ServiceLoader<IObfuscationService> serviceLoader;
   private final Set<IObfuscationService> services = new HashSet<>();

   private ObfuscationServices() {
      this.serviceLoader = ServiceLoader.load(IObfuscationService.class, this.getClass().getClassLoader());
   }

   public static ObfuscationServices getInstance() {
      if (instance == null) {
         instance = new ObfuscationServices();
      }

      return instance;
   }

   public void initProviders(IMixinAnnotationProcessor ap) {
      boolean defaultIsPresent = false;

      try {
         for (IObfuscationService service : this.serviceLoader) {
            if (!this.services.contains(service)) {
               this.services.add(service);
               String serviceName = service.getClass().getSimpleName();
               Collection<ObfuscationTypeDescriptor> obfTypes = service.getObfuscationTypes(ap);
               if (obfTypes != null) {
                  for (ObfuscationTypeDescriptor obfType : obfTypes) {
                     try {
                        ObfuscationType type = ObfuscationType.create(obfType, ap);
                        ap.printMessage(Kind.NOTE, serviceName + " supports type: \"" + type + "\"");
                        defaultIsPresent |= type.isDefault();
                     } catch (Exception var10) {
                        var10.printStackTrace();
                     }
                  }
               }
            }
         }
      } catch (ServiceConfigurationError var11) {
         ap.printMessage(Kind.ERROR, var11.getClass().getSimpleName() + ": " + var11.getMessage());
         var11.printStackTrace();
      }

      if (!defaultIsPresent) {
         String defaultEnv = ap.getOption("defaultObfuscationEnv");
         if (defaultEnv == null) {
            ap.printMessage(
               Kind.WARNING,
               "No default obfuscation environment was specified and \"searge\" is not available. Please ensure defaultObfuscationEnv is specified in your build configuration"
            );
         } else {
            ap.printMessage(
               Kind.WARNING,
               "Specified default obfuscation environment \""
                  + defaultEnv.toLowerCase(Locale.ROOT)
                  + "\" was not defined. This probably means your build configuration is out of date or a required service is missing"
            );
         }
      }
   }

   public Set<String> getSupportedOptions() {
      Set<String> supportedOptions = new HashSet<>();

      for (IObfuscationService provider : this.services) {
         Set<String> options = provider.getSupportedOptions();
         if (options != null) {
            supportedOptions.addAll(options);
         }
      }

      return supportedOptions;
   }

   public IObfuscationService getService(Class<? extends IObfuscationService> serviceClass) {
      for (IObfuscationService service : this.services) {
         if (serviceClass.getName().equals(service.getClass().getName())) {
            return service;
         }
      }

      return null;
   }
}
