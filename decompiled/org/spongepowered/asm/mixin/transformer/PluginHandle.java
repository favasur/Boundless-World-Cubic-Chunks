package org.spongepowered.asm.mixin.transformer;

import com.google.common.base.Strings;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;
import org.spongepowered.asm.service.IMixinService;

class PluginHandle {
   private static final Logger logger = LogManager.getLogger("mixin");
   private final MixinConfig parent;
   private final IMixinConfigPlugin plugin;
   private PluginHandle.CompatibilityMode mode = PluginHandle.CompatibilityMode.NORMAL;
   private Method mdPreApply;
   private Method mdPostApply;

   PluginHandle(MixinConfig parent, IMixinService service, String pluginClassName) {
      IMixinConfigPlugin plugin = null;
      if (!Strings.isNullOrEmpty(pluginClassName)) {
         try {
            Class<?> pluginClass = service.getClassProvider().findClass(pluginClassName, true);
            plugin = (IMixinConfigPlugin)pluginClass.newInstance();
         } catch (Throwable var6) {
            logger.error(
               "Error loading companion plugin class [{}] for mixin config [{}]. The plugin may be out of date: {}:{}",
               new Object[]{pluginClassName, parent, var6.getClass().getSimpleName(), var6.getMessage(), var6}
            );
            plugin = null;
         }
      }

      this.parent = parent;
      this.plugin = plugin;
   }

   IMixinConfigPlugin get() {
      return this.plugin;
   }

   boolean isAvailable() {
      return this.plugin != null;
   }

   void onLoad(String mixinPackage) {
      if (this.plugin != null) {
         this.plugin.onLoad(mixinPackage);
      }
   }

   String getRefMapperConfig() {
      return this.plugin != null ? this.plugin.getRefMapperConfig() : null;
   }

   List<String> getMixins() {
      return this.plugin != null ? this.plugin.getMixins() : null;
   }

   boolean shouldApplyMixin(String targetName, String className) {
      return this.plugin == null || this.plugin.shouldApplyMixin(targetName, className);
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) {
      if (this.plugin != null) {
         if (this.mode == PluginHandle.CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
         } else if (this.mode == PluginHandle.CompatibilityMode.COMPATIBLE) {
            try {
               this.applyLegacy(this.mdPreApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception var6) {
               this.mode = PluginHandle.CompatibilityMode.FAILED;
               throw var6;
            }
         } else {
            try {
               this.plugin.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (AbstractMethodError var7) {
               this.mode = PluginHandle.CompatibilityMode.COMPATIBLE;
               this.initReflection();
               this.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            }
         }
      }
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, MixinInfo mixinInfo) {
      if (this.plugin != null) {
         if (this.mode == PluginHandle.CompatibilityMode.FAILED) {
            throw new IllegalStateException("Companion plugin failure for [" + this.parent + "] plugin [" + this.plugin.getClass() + "]");
         } else if (this.mode == PluginHandle.CompatibilityMode.COMPATIBLE) {
            try {
               this.applyLegacy(this.mdPostApply, targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (Exception var6) {
               this.mode = PluginHandle.CompatibilityMode.FAILED;
               throw var6;
            }
         } else {
            try {
               this.plugin.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            } catch (AbstractMethodError var7) {
               this.mode = PluginHandle.CompatibilityMode.COMPATIBLE;
               this.initReflection();
               this.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
            }
         }
      }
   }

   private void initReflection() {
      if (this.mdPreApply == null) {
         try {
            Class<?> pluginClass = this.plugin.getClass();
            this.mdPreApply = pluginClass.getMethod("preApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class, IMixinInfo.class);
            this.mdPostApply = pluginClass.getMethod("postApply", String.class, org.spongepowered.asm.lib.tree.ClassNode.class, String.class, IMixinInfo.class);
         } catch (Throwable var2) {
            logger.catching(var2);
         }
      }
   }

   private void applyLegacy(Method method, String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
      try {
         method.invoke(this.plugin, targetClassName, new org.spongepowered.asm.lib.tree.ClassNode(targetClass), mixinClassName, mixinInfo);
      } catch (LinkageError var8) {
         throw new CompanionPluginError(this.apiError("Accessing [" + var8.getMessage() + "]"), var8);
      } catch (IllegalAccessException var9) {
         throw new CompanionPluginError(this.apiError("Fallback failed [" + var9.getMessage() + "]"), var9);
      } catch (IllegalArgumentException var10) {
         throw new CompanionPluginError(this.apiError("Fallback failed [" + var10.getMessage() + "]"), var10);
      } catch (InvocationTargetException var11) {
         Throwable th = (Throwable)(var11.getCause() != null ? var11.getCause() : var11);
         throw new CompanionPluginError(this.apiError("Fallback failed [" + th.getMessage() + "]"), th);
      }
   }

   private String apiError(String message) {
      return String.format("Companion plugin attempted to use a deprected API in [%s] plugin [%s]: %s", this.parent, this.plugin.getClass().getName(), message);
   }

   static enum CompatibilityMode {
      NORMAL,
      COMPATIBLE,
      FAILED;

      private CompatibilityMode() {
      }
   }
}
