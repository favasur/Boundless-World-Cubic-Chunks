package org.spongepowered.asm.service;

import java.io.InputStream;
import java.util.Collection;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.ReEntranceLock;

public interface IMixinService {
   String getName();

   boolean isValid();

   void prepare();

   MixinEnvironment.Phase getInitialPhase();

   void init();

   void beginPhase();

   void checkEnv(Object var1);

   ReEntranceLock getReEntranceLock();

   IClassProvider getClassProvider();

   IClassBytecodeProvider getBytecodeProvider();

   ITransformerProvider getTransformerProvider();

   IClassTracker getClassTracker();

   IMixinAuditTrail getAuditTrail();

   Collection<String> getPlatformAgents();

   IContainerHandle getPrimaryContainer();

   Collection<IContainerHandle> getMixinContainers();

   InputStream getResourceAsStream(String var1);

   String getSideName();

   MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel();

   MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel();
}
