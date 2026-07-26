package org.spongepowered.asm.launch.platform;

import cpw.mods.modlauncher.Environment;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment.Keys;
import cpw.mods.modlauncher.api.TypesafeMap.Key;
import java.util.Collection;
import java.util.Locale;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class MixinPlatformAgentMinecraftForge extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {
   public MixinPlatformAgentMinecraftForge() {
   }

   @Override
   public void init() {
   }

   @Override
   public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
      return IMixinPlatformAgent.AcceptResult.REJECTED;
   }

   @Override
   public String getSideName() {
      Environment environment = Launcher.INSTANCE.environment();
      String launchTarget = environment.getProperty((Key)Keys.LAUNCHTARGET.get()).orElse("missing").toLowerCase(Locale.ROOT);
      if (launchTarget.contains("server")) {
         return "SERVER";
      } else {
         return launchTarget.contains("client") ? "CLIENT" : null;
      }
   }

   @Override
   public Collection<IContainerHandle> getMixinContainers() {
      return null;
   }
}
