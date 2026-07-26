package org.spongepowered.asm.mixin.injection;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.IMessageSink;

public interface IInjectionPointContext extends IMessageSink {
   IMixinContext getContext();

   MethodNode getMethod();

   AnnotationNode getAnnotation();
}
