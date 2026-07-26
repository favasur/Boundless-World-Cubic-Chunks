package org.spongepowered.asm.service.modlauncher;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.service.IClassTracker;

public class ModLauncherClassTracker implements IClassProcessor, IClassTracker {
   private final Set<String> invalidClasses = new HashSet<>();
   private final Set<String> loadedClasses = new HashSet<>();

   public ModLauncherClassTracker() {
   }

   @Override
   public void registerInvalidClass(String className) {
      synchronized (this.invalidClasses) {
         this.invalidClasses.add(className);
      }
   }

   @Override
   public boolean isClassLoaded(String className) {
      synchronized (this.loadedClasses) {
         return this.loadedClasses.contains(className);
      }
   }

   @Override
   public String getClassRestrictions(String className) {
      return "";
   }

   @Override
   public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
      String name = classType.getClassName();
      synchronized (this.invalidClasses) {
         if (this.invalidClasses.contains(name)) {
            throw new NoClassDefFoundError(String.format("%s is invalid", name));
         }
      }

      return Phases.AFTER_ONLY;
   }

   @Override
   public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
      if ("classloading".equals(reason)) {
         synchronized (this.loadedClasses) {
            this.loadedClasses.add(classType.getClassName());
         }
      }

      return false;
   }
}
