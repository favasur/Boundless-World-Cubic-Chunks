package org.spongepowered.asm.mixin.struct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

public class AnnotatedMethodInfo implements IInjectionPointContext {
   protected static final Logger logger = LogManager.getLogger("mixin");
   private final IMixinContext context;
   protected final MethodNode method;
   protected final AnnotationNode annotation;

   public AnnotatedMethodInfo(IMixinContext mixin, MethodNode method, AnnotationNode annotation) {
      this.context = mixin;
      this.method = method;
      this.annotation = annotation;
   }

   @Override
   public final IMixinContext getContext() {
      return this.context;
   }

   @Override
   public final MethodNode getMethod() {
      return this.method;
   }

   @Override
   public final AnnotationNode getAnnotation() {
      return this.annotation;
   }

   @Override
   public void addMessage(String format, Object... args) {
      if (this.context.getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
         logger.warn(String.format(format, args));
      }
   }
}
