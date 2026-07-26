package org.spongepowered.asm.launch.platform;

import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;

public abstract class MixinPlatformAgentAbstract implements IMixinPlatformAgent {
   protected static final Logger logger = LogManager.getLogger("mixin");
   protected MixinPlatformManager manager;
   protected IContainerHandle handle;

   protected MixinPlatformAgentAbstract() {
   }

   @Override
   public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
      this.manager = manager;
      this.handle = handle;
      return IMixinPlatformAgent.AcceptResult.ACCEPTED;
   }

   @Override
   public String getPhaseProvider() {
      return null;
   }

   @Override
   public void prepare() {
   }

   @Override
   public void initPrimaryContainer() {
   }

   @Override
   public void inject() {
   }

   @Override
   public String toString() {
      return String.format("PlatformAgent[%s:%s]", this.getClass().getSimpleName(), this.handle);
   }

   protected static String invokeStringMethod(ClassLoader classLoader, String className, String methodName) {
      try {
         Class<?> clazz = Class.forName(className, false, classLoader);
         Method method = clazz.getDeclaredMethod(methodName);
         return ((Enum)method.invoke(null)).name();
      } catch (Exception var5) {
         return null;
      }
   }

   @Deprecated
   public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
   }

   @Deprecated
   public void unwire() {
   }
}
