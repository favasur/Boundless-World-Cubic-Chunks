package org.spongepowered.asm.launch;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.MixinService;

public final class GlobalProperties {
   private static IGlobalPropertyService service;

   private GlobalProperties() {
   }

   private static IGlobalPropertyService getService() {
      if (service == null) {
         service = MixinService.getGlobalPropertyService();
      }

      return service;
   }

   public static <T> T get(GlobalProperties.Keys key) {
      IGlobalPropertyService service = getService();
      return service.getProperty(key.resolve(service));
   }

   public static void put(GlobalProperties.Keys key, Object value) {
      IGlobalPropertyService service = getService();
      service.setProperty(key.resolve(service), value);
   }

   public static <T> T get(GlobalProperties.Keys key, T defaultValue) {
      IGlobalPropertyService service = getService();
      return service.getProperty(key.resolve(service), defaultValue);
   }

   public static String getString(GlobalProperties.Keys key, String defaultValue) {
      IGlobalPropertyService service = getService();
      return service.getPropertyString(key.resolve(service), defaultValue);
   }

   public static final class Keys {
      public static final GlobalProperties.Keys INIT = of("mixin.initialised");
      public static final GlobalProperties.Keys AGENTS = of("mixin.agents");
      public static final GlobalProperties.Keys CONFIGS = of("mixin.configs");
      public static final GlobalProperties.Keys PLATFORM_MANAGER = of("mixin.platform");
      public static final GlobalProperties.Keys FML_LOAD_CORE_MOD = of("mixin.launch.fml.loadcoremodmethod");
      public static final GlobalProperties.Keys FML_GET_REPARSEABLE_COREMODS = of("mixin.launch.fml.reparseablecoremodsmethod");
      public static final GlobalProperties.Keys FML_CORE_MOD_MANAGER = of("mixin.launch.fml.coremodmanagerclass");
      public static final GlobalProperties.Keys FML_GET_IGNORED_MODS = of("mixin.launch.fml.ignoredmodsmethod");
      private static Map<String, GlobalProperties.Keys> keys;
      private final String name;
      private IPropertyKey key;

      private Keys(String name) {
         this.name = name;
      }

      IPropertyKey resolve(IGlobalPropertyService service) {
         if (this.key != null) {
            return this.key;
         } else {
            return service == null ? null : (this.key = service.resolveKey(this.name));
         }
      }

      public static GlobalProperties.Keys of(String name) {
         if (keys == null) {
            keys = new HashMap<>();
         }

         GlobalProperties.Keys key = keys.get(name);
         if (key == null) {
            key = new GlobalProperties.Keys(name);
            keys.put(name, key);
         }

         return key;
      }
   }
}
