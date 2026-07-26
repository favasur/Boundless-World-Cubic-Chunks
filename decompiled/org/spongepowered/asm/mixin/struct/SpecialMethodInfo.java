package org.spongepowered.asm.mixin.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MethodNodeEx;

public class SpecialMethodInfo extends AnnotatedMethodInfo {
   protected final String annotationType;
   protected final ClassNode classNode;
   protected final String methodName;
   protected final MixinTargetContext mixin;

   public SpecialMethodInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation);
      this.mixin = mixin;
      this.annotationType = this.annotation != null ? "@" + Bytecode.getSimpleName(this.annotation) : "Undecorated injector";
      this.classNode = mixin.getTargetClassNode();
      this.methodName = MethodNodeEx.getName(method);
   }

   public final ClassNode getClassNode() {
      return this.classNode;
   }

   public final ClassInfo getClassInfo() {
      return this.mixin.getClassInfo();
   }

   public String getMethodName() {
      return this.methodName;
   }
}
