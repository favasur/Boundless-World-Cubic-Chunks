package org.spongepowered.asm.mixin.transformer;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

public class Config {
   private static final Logger logger = LogManager.getLogger("mixin");
   private static final Map<String, Config> allConfigs = new HashMap<>();
   private final String name;
   private final MixinConfig config;

   public Config(MixinConfig config) {
      this.name = config.getName();
      this.config = config;
   }

   public String getName() {
      return this.name;
   }

   MixinConfig get() {
      return this.config;
   }

   public boolean isVisited() {
      return this.config.isVisited();
   }

   public IMixinConfig getConfig() {
      return this.config;
   }

   public MixinEnvironment getEnvironment() {
      return this.config.getEnvironment();
   }

   public Config getParent() {
      MixinConfig parent = this.config.getParent();
      return parent != null ? parent.getHandle() : null;
   }

   @Override
   public String toString() {
      return this.config.toString();
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof Config && this.name.equals(((Config)obj).name);
   }

   @Override
   public int hashCode() {
      return this.name.hashCode();
   }

   @Deprecated
   public static Config create(String configFile, MixinEnvironment outer) {
      Config config = allConfigs.get(configFile);
      if (config != null) {
         return config;
      } else {
         try {
            config = MixinConfig.create(configFile, outer);
            if (config != null) {
               allConfigs.put(config.getName(), config);
            }
         } catch (Exception var7) {
            throw new MixinInitialisationError("Error initialising mixin config " + configFile, var7);
         }

         if (config == null) {
            return null;
         } else {
            String parent = config.get().getParentName();
            if (!Strings.isNullOrEmpty(parent)) {
               Config parentConfig;
               try {
                  parentConfig = create(parent, outer);
                  if (parentConfig != null && !config.get().assignParent(parentConfig)) {
                     config = null;
                  }
               } catch (Throwable var6) {
                  throw new MixinInitialisationError("Error initialising parent mixin config " + parent + " of " + configFile, var6);
               }

               if (parentConfig == null) {
                  logger.error("Error encountered initialising mixin config {0}: The parent {1} could not be read.", new Object[]{configFile, parent});
               }
            }

            return config;
         }
      }
   }

   public static Config create(String configFile) {
      return MixinConfig.create(configFile, MixinEnvironment.getDefaultEnvironment());
   }
}
