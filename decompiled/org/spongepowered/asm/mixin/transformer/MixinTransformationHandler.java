package org.spongepowered.asm.mixin.transformer;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.IClassProcessor;
import org.spongepowered.asm.launch.Phases;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

public class MixinTransformationHandler implements IClassProcessor {
   private final Object initialisationLock = new Object();
   private MixinTransformer transformer;
   private ISyntheticClassRegistry registry;

   public MixinTransformationHandler() {
   }

   @Override
   public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
      if (!isEmpty) {
         return Phases.AFTER_ONLY;
      } else if (this.registry == null) {
         return null;
      } else {
         ISyntheticClassInfo syntheticClass = this.registry.findSyntheticClass(classType.getClassName());
         return syntheticClass != null ? Phases.AFTER_ONLY : null;
      }
   }

   @Override
   public synchronized boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
      if (phase == Phase.BEFORE) {
         return false;
      } else {
         MixinTransformer transformer = null;
         if (this.transformer == null) {
            synchronized (this.initialisationLock) {
               transformer = this.transformer;
               if (transformer == null) {
                  transformer = this.transformer = new MixinTransformer();
                  this.registry = transformer.getExtensions().getSyntheticClassRegistry();
               }
            }
         } else {
            transformer = this.transformer;
         }

         if ("mixin".equals(reason)) {
            return false;
         } else {
            MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
            ISyntheticClassInfo syntheticClass = this.registry.findSyntheticClass(classType.getClassName());
            if (syntheticClass != null) {
               return transformer.generateClass(environment, classType.getClassName(), classNode);
            } else {
               return "computing_frames".equals(reason)
                  ? transformer.computeFramesForClass(environment, classType.getClassName(), classNode)
                  : transformer.transformClass(environment, classType.getClassName(), classNode);
            }
         }
      }
   }
}
