package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.struct.SourceMap;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;

final class TargetClassContext extends ClassContext implements ITargetClassContext {
   private static final Logger logger = LogManager.getLogger("mixin");
   private final MixinEnvironment env;
   private final Extensions extensions;
   private final String sessionId;
   private final String className;
   private final ClassNode classNode;
   private final ClassInfo classInfo;
   private final SourceMap sourceMap;
   private final ClassSignature signature;
   private final SortedSet<MixinInfo> mixins;
   private final Map<String, Target> targetMethods = new HashMap<>();
   private final Set<MethodNode> mixinMethods = new HashSet<>();
   private final List<InvalidMixinException> suppressedExceptions = new ArrayList<>();
   private boolean applied;
   private boolean forceExport;

   TargetClassContext(MixinEnvironment env, Extensions extensions, String sessionId, String name, ClassNode classNode, SortedSet<MixinInfo> mixins) {
      this.env = env;
      this.extensions = extensions;
      this.sessionId = sessionId;
      this.className = name;
      this.classNode = classNode;
      this.classInfo = ClassInfo.fromClassNode(classNode);
      this.signature = this.classInfo.getSignature();
      this.mixins = mixins;
      this.sourceMap = new SourceMap(classNode.sourceFile);
      this.sourceMap.addFile(this.classNode);
   }

   @Override
   public String toString() {
      return this.className;
   }

   boolean isApplied() {
      return this.applied;
   }

   boolean isExportForced() {
      return this.forceExport;
   }

   Extensions getExtensions() {
      return this.extensions;
   }

   String getSessionId() {
      return this.sessionId;
   }

   @Override
   String getClassRef() {
      return this.classNode.name;
   }

   String getClassName() {
      return this.className;
   }

   @Override
   public ClassNode getClassNode() {
      return this.classNode;
   }

   List<MethodNode> getMethods() {
      return this.classNode.methods;
   }

   List<FieldNode> getFields() {
      return this.classNode.fields;
   }

   @Override
   public ClassInfo getClassInfo() {
      return this.classInfo;
   }

   SortedSet<MixinInfo> getMixins() {
      return this.mixins;
   }

   SourceMap getSourceMap() {
      return this.sourceMap;
   }

   void mergeSignature(ClassSignature signature) {
      this.signature.merge(signature);
   }

   void addMixinMethod(MethodNode method) {
      this.mixinMethods.add(method);
   }

   void methodMerged(MethodNode method) {
      if (!this.mixinMethods.remove(method)) {
         logger.debug("Unexpected: Merged unregistered method {}{} in {}", new Object[]{method.name, method.desc, this});
      }
   }

   MethodNode findMethod(Deque<String> aliases, String desc) {
      return this.findAliasedMethod(aliases, desc, true);
   }

   MethodNode findAliasedMethod(Deque<String> aliases, String desc) {
      return this.findAliasedMethod(aliases, desc, false);
   }

   private MethodNode findAliasedMethod(Deque<String> aliases, String desc, boolean includeMixinMethods) {
      String alias = aliases.poll();
      if (alias == null) {
         return null;
      } else {
         for (MethodNode target : this.classNode.methods) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
               return target;
            }
         }

         if (includeMixinMethods) {
            for (MethodNode targetx : this.mixinMethods) {
               if (targetx.name.equals(alias) && targetx.desc.equals(desc)) {
                  return targetx;
               }
            }
         }

         return this.findAliasedMethod(aliases, desc);
      }
   }

   FieldNode findAliasedField(Deque<String> aliases, String desc) {
      String alias = aliases.poll();
      if (alias == null) {
         return null;
      } else {
         for (FieldNode target : this.classNode.fields) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
               return target;
            }
         }

         return this.findAliasedField(aliases, desc);
      }
   }

   Target getTargetMethod(MethodNode method) {
      if (!this.classNode.methods.contains(method)) {
         throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
      } else {
         String targetName = method.name + method.desc;
         Target target = this.targetMethods.get(targetName);
         if (target == null) {
            target = new Target(this.classNode, method);
            this.targetMethods.put(targetName, target);
         }

         return target;
      }
   }

   void applyMixins() {
      if (this.applied) {
         throw new IllegalStateException("Mixins already applied to target class " + this.className);
      } else {
         this.applied = true;
         MixinApplicatorStandard applicator = this.createApplicator();
         applicator.apply(this.mixins);
         this.applySignature();
         this.upgradeMethods();
         this.checkMerges();
      }
   }

   private MixinApplicatorStandard createApplicator() {
      return (MixinApplicatorStandard)(this.classInfo.isInterface() ? new MixinApplicatorInterface(this) : new MixinApplicatorStandard(this));
   }

   private void applySignature() {
      this.classNode.signature = this.signature.toString();
   }

   private void checkMerges() {
      for (MethodNode method : this.mixinMethods) {
         if (!method.name.startsWith("<")) {
            logger.debug("Unexpected: Registered method {}{} in {} was not merged", new Object[]{method.name, method.desc, this});
         }
      }
   }

   void processDebugTasks() {
      AnnotationNode classDebugAnnotation = Annotations.getVisible(this.classNode, Debug.class);
      this.forceExport = classDebugAnnotation != null && Boolean.TRUE.equals(Annotations.getValue(classDebugAnnotation, "export"));
      if (this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
         if (classDebugAnnotation != null && Boolean.TRUE.equals(Annotations.getValue(classDebugAnnotation, "print"))) {
            Bytecode.textify(this.classNode, System.err);
         }

         for (MethodNode method : this.classNode.methods) {
            AnnotationNode methodDebugAnnotation = Annotations.getVisible(method, Debug.class);
            if (methodDebugAnnotation != null && Boolean.TRUE.equals(Annotations.getValue(methodDebugAnnotation, "print"))) {
               Bytecode.textify(method, System.err);
            }
         }
      }
   }

   void addSuppressed(InvalidMixinException ex) {
      this.suppressedExceptions.add(ex);
   }

   List<InvalidMixinException> getSuppressedExceptions() {
      return this.suppressedExceptions;
   }
}
