package org.spongepowered.asm.mixin.transformer;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinReloadException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTargetAlreadyLoadedException;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.perf.Profiler;

class MixinInfo implements Comparable<MixinInfo>, IMixinInfo {
   static int mixinOrder = 0;
   private final transient Logger logger = LogManager.getLogger("mixin");
   private final transient Profiler profiler = MixinEnvironment.getProfiler();
   private final transient MixinConfig parent;
   private final String name;
   private final String className;
   private final int priority;
   private final boolean virtual;
   private final transient List<MixinInfo.DeclaredTarget> declaredTargets;
   private final transient List<ClassInfo> targetClasses = new ArrayList<>();
   private final List<String> targetClassNames = new ArrayList<>();
   private final transient int order = mixinOrder++;
   private final transient IMixinService service;
   private final transient PluginHandle plugin;
   private final transient MixinEnvironment.Phase phase;
   private final transient ClassInfo info;
   private final transient MixinInfo.SubType type;
   private final transient boolean strict;
   private transient MixinInfo.State pendingState;
   private transient MixinInfo.State state;

   MixinInfo(IMixinService service, MixinConfig parent, String name, PluginHandle plugin, boolean ignorePlugin) {
      this.service = service;
      this.parent = parent;
      this.name = name;
      this.className = parent.getMixinPackage() + name;
      this.plugin = plugin;
      this.phase = parent.getEnvironment().getPhase();
      this.strict = parent.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_TARGETS);

      try {
         ClassNode mixinClassNode = this.loadMixinClass(this.className);
         this.pendingState = new MixinInfo.State(mixinClassNode);
         this.info = this.pendingState.getClassInfo();
         this.type = MixinInfo.SubType.getTypeFor(this);
      } catch (InvalidMixinException var9) {
         throw var9;
      } catch (Exception var10) {
         throw new InvalidMixinException(this, var10.getMessage(), var10);
      }

      if (!this.type.isLoadable()) {
         IClassTracker tracker = this.service.getClassTracker();
         if (tracker != null) {
            tracker.registerInvalidClass(this.className);
         }
      }

      try {
         this.priority = this.readPriority(this.pendingState.getClassNode());
         this.virtual = this.readPseudo(this.pendingState.getValidationClassNode());
         this.declaredTargets = this.readDeclaredTargets(this.pendingState.getValidationClassNode(), ignorePlugin);
      } catch (InvalidMixinException var7) {
         throw var7;
      } catch (Exception var8) {
         throw new InvalidMixinException(this, var8);
      }
   }

   void parseTargets() {
      try {
         this.targetClasses.addAll(this.readTargetClasses(this.declaredTargets));
         this.targetClassNames.addAll(Lists.transform(this.targetClasses, Functions.toStringFunction()));
      } catch (InvalidMixinException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new InvalidMixinException(this, var3);
      }
   }

   void validate() {
      if (this.pendingState == null) {
         throw new IllegalStateException("No pending validation state for " + this);
      } else {
         try {
            this.pendingState.validate(this.type, this.targetClasses);
            this.state = this.pendingState;
         } finally {
            this.pendingState = null;
         }
      }
   }

   protected List<MixinInfo.DeclaredTarget> readDeclaredTargets(MixinInfo.MixinClassNode classNode, boolean ignorePlugin) {
      if (classNode == null) {
         return Collections.emptyList();
      } else {
         AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
         if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
         } else {
            IClassTracker tracker = this.service.getClassTracker();
            List<MixinInfo.DeclaredTarget> declaredTargets = new ArrayList<>();

            for (Object target : this.readTargets(mixin)) {
               MixinInfo.DeclaredTarget declaredTarget = MixinInfo.DeclaredTarget.of(target, this);
               if (declaredTarget != null) {
                  if (tracker != null && tracker.isClassLoaded(declaredTarget.name) && !this.isReloading()) {
                     String message = String.format("Critical problem: %s target %s was loaded too early.", this, declaredTarget.name);
                     if (this.parent.isRequired()) {
                        throw new MixinTargetAlreadyLoadedException(this, message, declaredTarget.name);
                     }

                     this.logger.error(message);
                  }

                  if (this.shouldApplyMixin(ignorePlugin, declaredTarget.name)) {
                     declaredTargets.add(declaredTarget);
                  }
               }
            }

            return declaredTargets;
         }
      }
   }

   private Iterable<Object> readTargets(AnnotationNode mixin) {
      Iterable<Object> publicTargets = Annotations.getValue(mixin, "value");
      Iterable<Object> privateTargets = Annotations.getValue(mixin, "targets");
      if (publicTargets == null && privateTargets == null) {
         return Collections.emptyList();
      } else if (publicTargets == null) {
         return privateTargets;
      } else {
         return privateTargets == null ? publicTargets : Iterables.concat(publicTargets, privateTargets);
      }
   }

   private boolean shouldApplyMixin(boolean ignorePlugin, String targetName) {
      Profiler.Section pluginTimer = this.profiler.begin("plugin");
      boolean result = ignorePlugin || this.plugin.shouldApplyMixin(targetName, this.className);
      pluginTimer.end();
      return result;
   }

   List<ClassInfo> readTargetClasses(MixinInfo.MixinClassNode classNode, boolean ignorePlugin) {
      return this.readTargetClasses(this.readDeclaredTargets(classNode, ignorePlugin));
   }

   private List<ClassInfo> readTargetClasses(List<MixinInfo.DeclaredTarget> declaredTargets) throws InvalidMixinException {
      List<ClassInfo> targetClasses = new ArrayList<>();

      for (MixinInfo.DeclaredTarget target : declaredTargets) {
         ClassInfo targetClass = this.getTargetClass(target);
         if (targetClass != null) {
            targetClasses.add(targetClass);
            targetClass.addMixin(this);
         }
      }

      return targetClasses;
   }

   private ClassInfo getTargetClass(MixinInfo.DeclaredTarget target) throws InvalidMixinException {
      ClassInfo targetInfo = ClassInfo.forName(target.name);
      if (targetInfo == null) {
         if (this.isVirtual()) {
            this.logger.debug("Skipping virtual target {} for {}", new Object[]{target.name, this});
         } else {
            this.handleTargetError(String.format("@Mixin target %s was not found %s", target.name, this));
         }

         return null;
      } else {
         this.type.validateTarget(target.name, targetInfo);
         if (target.isPrivate && targetInfo.isPublic() && !this.isVirtual()) {
            this.handleTargetError(String.format("@Mixin target %s is public in %s and should be specified in value", target.name, this));
         }

         return targetInfo;
      }
   }

   private void handleTargetError(String message) {
      if (this.strict) {
         this.logger.error(message);
         throw new InvalidMixinException(this, message);
      } else {
         this.logger.warn(message);
      }
   }

   protected int readPriority(ClassNode classNode) {
      if (classNode == null) {
         return this.parent.getDefaultMixinPriority();
      } else {
         AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
         if (mixin == null) {
            throw new InvalidMixinException(this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
         } else {
            Integer priority = Annotations.getValue(mixin, "priority");
            return priority == null ? this.parent.getDefaultMixinPriority() : priority;
         }
      }
   }

   protected boolean readPseudo(ClassNode classNode) {
      return Annotations.getInvisible(classNode, Pseudo.class) != null;
   }

   private boolean isReloading() {
      return this.pendingState instanceof MixinInfo.Reloaded;
   }

   String remapClassName(String className) {
      return this.parent.remapClassName(this.getClassRef(), className);
   }

   public boolean hasDeclaredTarget(String targetClass) {
      for (MixinInfo.DeclaredTarget declaredTarget : this.declaredTargets) {
         if (targetClass.equals(declaredTarget.name)) {
            return true;
         }
      }

      return false;
   }

   private MixinInfo.State getState() {
      return this.state != null ? this.state : this.pendingState;
   }

   ClassInfo getClassInfo() {
      return this.info;
   }

   @Override
   public IMixinConfig getConfig() {
      return this.parent;
   }

   MixinConfig getParent() {
      return this.parent;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getClassName() {
      return this.className;
   }

   @Override
   public String getClassRef() {
      return this.getClassInfo().getName();
   }

   @Override
   public byte[] getClassBytes() {
      throw new RuntimeException("NO");
   }

   @Override
   public boolean isDetachedSuper() {
      return this.getState().isDetachedSuper();
   }

   public boolean isUnique() {
      return this.getState().isUnique();
   }

   public boolean isVirtual() {
      return this.virtual;
   }

   public boolean isAccessor() {
      return this.type instanceof MixinInfo.SubType.Accessor;
   }

   public boolean isLoadable() {
      return this.type.isLoadable();
   }

   public boolean isRequired() {
      return this.parent.isRequired();
   }

   public Level getLoggingLevel() {
      return this.parent.getLoggingLevel();
   }

   @Override
   public MixinEnvironment.Phase getPhase() {
      return this.phase;
   }

   public MixinInfo.MixinClassNode getClassNode(int flags) {
      return this.getState().createClassNode(flags);
   }

   List<String> getDeclaredTargetClasses() {
      return Collections.unmodifiableList(Lists.transform(this.declaredTargets, Functions.toStringFunction()));
   }

   @Override
   public List<String> getTargetClasses() {
      return Collections.unmodifiableList(this.targetClassNames);
   }

   List<InterfaceInfo> getSoftImplements() {
      return Collections.unmodifiableList(this.getState().getSoftImplements());
   }

   Set<String> getSyntheticInnerClasses() {
      return Collections.unmodifiableSet(this.getState().getSyntheticInnerClasses());
   }

   Set<String> getInnerClasses() {
      return Collections.unmodifiableSet(this.getState().getInnerClasses());
   }

   List<ClassInfo> getTargets() {
      return Collections.unmodifiableList(this.targetClasses);
   }

   Set<String> getInterfaces() {
      return this.getState().getInterfaces();
   }

   MixinTargetContext createContextFor(TargetClassContext target) {
      MixinInfo.MixinClassNode classNode = this.getClassNode(8);
      Profiler.Section preTimer = this.profiler.begin("pre");
      MixinTargetContext context = this.type.createPreProcessor(classNode).prepare().createContextFor(target);
      preTimer.end();
      return context;
   }

   private ClassNode loadMixinClass(String mixinClassName) throws ClassNotFoundException {
      ClassNode classNode = null;

      try {
         IClassTracker tracker = this.service.getClassTracker();
         if (tracker != null) {
            String restrictions = tracker.getClassRestrictions(mixinClassName);
            if (restrictions.length() > 0) {
               this.logger.error("Classloader restrictions [{}] encountered loading {}, name: {}", new Object[]{restrictions, this, mixinClassName});
            }
         }

         return this.service.getBytecodeProvider().getClassNode(mixinClassName, true);
      } catch (ClassNotFoundException var5) {
         throw new ClassNotFoundException(String.format("The specified mixin '%s' was not found", mixinClassName));
      } catch (IOException var6) {
         this.logger.warn("Failed to load mixin {}, the specified mixin will not be applied", new Object[]{mixinClassName});
         throw new InvalidMixinException(this, "An error was encountered whilst loading the mixin class", var6);
      }
   }

   void reloadMixin(ClassNode classNode) {
      if (this.pendingState != null) {
         throw new IllegalStateException("Cannot reload mixin while it is initialising");
      } else {
         this.pendingState = new MixinInfo.Reloaded(this.state, classNode);
         this.validate();
      }
   }

   public int compareTo(MixinInfo other) {
      if (other == null) {
         return 0;
      } else {
         return other.priority == this.priority ? this.order - other.order : this.priority - other.priority;
      }
   }

   public void preApply(String transformedName, ClassNode targetClass) {
      if (this.plugin.isAvailable()) {
         Profiler.Section pluginTimer = this.profiler.begin("plugin");

         try {
            this.plugin.preApply(transformedName, targetClass, this.className, this);
         } finally {
            pluginTimer.end();
         }
      }
   }

   public void postApply(String transformedName, ClassNode targetClass) {
      if (this.plugin.isAvailable()) {
         Profiler.Section pluginTimer = this.profiler.begin("plugin");

         try {
            this.plugin.postApply(transformedName, targetClass, this.className, this);
         } finally {
            pluginTimer.end();
         }
      }

      this.parent.postApply(transformedName, targetClass);
      this.info.addAppliedMixin(this);
   }

   @Override
   public String toString() {
      return String.format("%s:%s", this.parent.getName(), this.name);
   }

   static MixinInfo.Variant getVariant(ClassNode classNode) {
      return getVariant(ClassInfo.fromClassNode(classNode));
   }

   static MixinInfo.Variant getVariant(ClassInfo classInfo) {
      if (!classInfo.isInterface()) {
         return MixinInfo.Variant.STANDARD;
      } else {
         boolean containsNonAccessorMethod = false;

         for (ClassInfo.Method method : classInfo.getMethods()) {
            containsNonAccessorMethod |= !method.isAccessor() && !method.isSynthetic();
         }

         return containsNonAccessorMethod ? MixinInfo.Variant.INTERFACE : MixinInfo.Variant.ACCESSOR;
      }
   }

   static final class DeclaredTarget {
      final String name;
      final boolean isPrivate;

      private DeclaredTarget(String name, boolean isPrivate) {
         this.name = name;
         this.isPrivate = isPrivate;
      }

      @Override
      public String toString() {
         return this.name;
      }

      static MixinInfo.DeclaredTarget of(Object target, MixinInfo info) {
         if (target instanceof String) {
            String remappedName = info.remapClassName((String)target);
            return remappedName != null ? new MixinInfo.DeclaredTarget(remappedName, true) : null;
         } else {
            return target instanceof Type ? new MixinInfo.DeclaredTarget(((Type)target).getClassName(), false) : null;
         }
      }
   }

   class MixinClassNode extends ClassNode {
      public final List<MixinInfo.MixinMethodNode> mixinMethods = this.methods;

      MixinClassNode(MixinInfo mixin) {
         this(ASM.API_VERSION);
      }

      protected MixinClassNode(int api) {
         super(api);
      }

      public MixinInfo getMixin() {
         return MixinInfo.this;
      }

      public List<FieldNode> getFields() {
         return new ArrayList<>(this.fields);
      }

      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
         MethodNode method = MixinInfo.this.new MixinMethodNode(access, name, desc, signature, exceptions);
         this.methods.add(method);
         return method;
      }
   }

   class MixinMethodNode extends MethodNodeEx {
      public MixinMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
         super(access, name, desc, signature, exceptions, MixinInfo.this);
      }

      public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
         Object[] bsmArgs = new Object[bootstrapMethodArguments.length];
         System.arraycopy(bootstrapMethodArguments, 0, bsmArgs, 0, bootstrapMethodArguments.length);
         this.instructions.add(new InvokeDynamicInsnNode(name, descriptor, bootstrapMethodHandle, bsmArgs));
      }

      public boolean isInjector() {
         return this.getInjectorAnnotation() != null || this.isSurrogate();
      }

      public boolean isSurrogate() {
         return this.getVisibleAnnotation(Surrogate.class) != null;
      }

      public boolean isSynthetic() {
         return Bytecode.hasFlag(this, 4096);
      }

      public AnnotationNode getVisibleAnnotation(Class<? extends Annotation> annotationClass) {
         return Annotations.getVisible(this, annotationClass);
      }

      public AnnotationNode getInjectorAnnotation() {
         return InjectionInfo.getInjectorAnnotation(MixinInfo.this, this);
      }
   }

   class Reloaded extends MixinInfo.State {
      private final MixinInfo.State previous;

      Reloaded(MixinInfo.State previous, ClassNode classNode) {
         super(classNode, previous.getClassInfo());
         this.previous = previous;
      }

      @Override
      protected void validateChanges(MixinInfo.SubType type, List<ClassInfo> targetClasses) {
         if (!this.syntheticInnerClasses.equals(this.previous.syntheticInnerClasses)) {
            throw new MixinReloadException(MixinInfo.this, "Cannot change inner classes");
         } else if (!this.interfaces.equals(this.previous.interfaces)) {
            throw new MixinReloadException(MixinInfo.this, "Cannot change interfaces");
         } else if (!new HashSet<>(this.softImplements).equals(new HashSet<>(this.previous.softImplements))) {
            throw new MixinReloadException(MixinInfo.this, "Cannot change soft interfaces");
         } else {
            List<ClassInfo> targets = MixinInfo.this.readTargetClasses(this.validationClassNode, true);
            if (!new HashSet<>(targets).equals(new HashSet<>(targetClasses))) {
               throw new MixinReloadException(MixinInfo.this, "Cannot change target classes");
            } else {
               int priority = MixinInfo.this.readPriority(this.validationClassNode);
               if (priority != MixinInfo.this.getPriority()) {
                  throw new MixinReloadException(MixinInfo.this, "Cannot change mixin priority");
               }
            }
         }
      }
   }

   class State {
      private final ClassNode classNode;
      private final ClassInfo classInfo;
      private boolean detachedSuper;
      private boolean unique;
      protected final Set<String> interfaces = new HashSet<>();
      protected final List<InterfaceInfo> softImplements = new ArrayList<>();
      protected final Set<String> syntheticInnerClasses = new HashSet<>();
      protected final Set<String> innerClasses = new HashSet<>();
      protected MixinInfo.MixinClassNode validationClassNode;

      State(ClassNode classNode) {
         this(classNode, null);
      }

      State(ClassNode classNode, ClassInfo classInfo) {
         this.classNode = classNode;
         this.connect();
         this.classInfo = classInfo != null ? classInfo : ClassInfo.fromClassNode(this.getValidationClassNode());
      }

      protected void connect() {
         this.validationClassNode = this.createClassNode(0);
      }

      protected void complete() {
         this.validationClassNode = null;
      }

      ClassInfo getClassInfo() {
         return this.classInfo;
      }

      ClassNode getClassNode() {
         return this.classNode;
      }

      MixinInfo.MixinClassNode getValidationClassNode() {
         if (this.validationClassNode == null) {
            throw new IllegalStateException("Attempted a validation task after validation is complete on " + this + " in " + MixinInfo.this);
         } else {
            return this.validationClassNode;
         }
      }

      boolean isDetachedSuper() {
         return this.detachedSuper;
      }

      boolean isUnique() {
         return this.unique;
      }

      List<? extends InterfaceInfo> getSoftImplements() {
         return this.softImplements;
      }

      Set<String> getSyntheticInnerClasses() {
         return this.syntheticInnerClasses;
      }

      Set<String> getInnerClasses() {
         return this.innerClasses;
      }

      Set<String> getInterfaces() {
         return this.interfaces;
      }

      MixinInfo.MixinClassNode createClassNode(int flags) {
         MixinInfo.MixinClassNode mixinClassNode = MixinInfo.this.new MixinClassNode(MixinInfo.this);
         this.classNode.accept(mixinClassNode);
         return mixinClassNode;
      }

      void validate(MixinInfo.SubType type, List<ClassInfo> targetClasses) {
         MixinInfo.MixinClassNode classNode = this.getValidationClassNode();
         MixinPreProcessorStandard preProcessor = type.createPreProcessor(classNode).prepare();

         for (ClassInfo target : targetClasses) {
            preProcessor.conform(target);
         }

         type.validate(this, targetClasses);
         this.detachedSuper = type.isDetachedSuper();
         this.unique = Annotations.getVisible(classNode, Unique.class) != null;
         this.validateInner();
         this.validateClassVersion();
         this.validateRemappables(targetClasses);
         this.readImplementations(type);
         this.readInnerClasses();
         this.validateChanges(type, targetClasses);
         this.complete();
      }

      private void validateInner() {
         if (!this.classInfo.isProbablyStatic()) {
            throw new InvalidMixinException(MixinInfo.this, "Inner class mixin must be declared static");
         }
      }

      private void validateClassVersion() {
         if (this.validationClassNode.version > MixinEnvironment.getCompatibilityLevel().classVersion()) {
            String helpText = ".";

            for (MixinEnvironment.CompatibilityLevel level : MixinEnvironment.CompatibilityLevel.values()) {
               if (level.classVersion() >= this.validationClassNode.version) {
                  helpText = String.format(". Mixin requires compatibility level %s or above.", level.name());
               }
            }

            throw new InvalidMixinException(MixinInfo.this, "Unsupported mixin class version " + this.validationClassNode.version + helpText);
         }
      }

      private void validateRemappables(List<ClassInfo> targetClasses) {
         if (targetClasses.size() > 1) {
            for (FieldNode field : this.validationClassNode.fields) {
               this.validateRemappable(Shadow.class, field.name, Annotations.getVisible(field, Shadow.class));
            }

            for (MethodNode method : this.validationClassNode.methods) {
               this.validateRemappable(Shadow.class, method.name, Annotations.getVisible(method, Shadow.class));
               AnnotationNode overwrite = Annotations.getVisible(method, Overwrite.class);
               if (overwrite != null && ((method.access & 8) == 0 || (method.access & 1) == 0)) {
                  throw new InvalidMixinException(MixinInfo.this, "Found @Overwrite annotation on " + method.name + " in " + MixinInfo.this);
               }
            }
         }
      }

      private void validateRemappable(Class<Shadow> annotationClass, String name, AnnotationNode annotation) {
         if (annotation != null && Annotations.getValue(annotation, "remap", Boolean.TRUE)) {
            throw new InvalidMixinException(MixinInfo.this, "Found a remappable @" + annotationClass.getSimpleName() + " annotation on " + name + " in " + this);
         }
      }

      void readImplementations(MixinInfo.SubType type) {
         this.interfaces.addAll(this.validationClassNode.interfaces);
         this.interfaces.addAll(type.getInterfaces());
         AnnotationNode implementsAnnotation = Annotations.getInvisible(this.validationClassNode, Implements.class);
         if (implementsAnnotation != null) {
            List<AnnotationNode> interfaces = Annotations.getValue(implementsAnnotation);
            if (interfaces != null) {
               for (AnnotationNode interfaceNode : interfaces) {
                  InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(MixinInfo.this, interfaceNode);
                  this.softImplements.add(interfaceInfo);
                  this.interfaces.add(interfaceInfo.getInternalName());
                  if (!(this instanceof MixinInfo.Reloaded)) {
                     this.classInfo.addInterface(interfaceInfo.getInternalName());
                  }
               }
            }
         }
      }

      void readInnerClasses() {
         for (InnerClassNode inner : this.validationClassNode.innerClasses) {
            ClassInfo innerClass = ClassInfo.forName(inner.name);
            if (inner.outerName != null && inner.outerName.equals(this.classInfo.getName()) || inner.name.startsWith(this.validationClassNode.name + "$")) {
               if (innerClass.isProbablyStatic() && innerClass.isSynthetic()) {
                  this.syntheticInnerClasses.add(inner.name);
               } else {
                  this.innerClasses.add(inner.name);
               }
            }
         }
      }

      protected void validateChanges(MixinInfo.SubType type, List<ClassInfo> targetClasses) {
         type.createPreProcessor(this.validationClassNode).prepare();
      }
   }

   abstract static class SubType {
      protected final MixinInfo mixin;
      protected final String annotationType;
      protected final boolean targetMustBeInterface;
      protected boolean detached;

      SubType(MixinInfo info, String annotationType, boolean targetMustBeInterface) {
         this.mixin = info;
         this.annotationType = annotationType;
         this.targetMustBeInterface = targetMustBeInterface;
      }

      Collection<String> getInterfaces() {
         return Collections.emptyList();
      }

      boolean isDetachedSuper() {
         return this.detached;
      }

      boolean isLoadable() {
         return false;
      }

      void validateTarget(String targetName, ClassInfo targetInfo) {
         boolean targetIsInterface = targetInfo.isInterface();
         if (targetIsInterface != this.targetMustBeInterface) {
            String not = targetIsInterface ? "" : "not ";
            throw new InvalidMixinException(this.mixin, this.annotationType + " target type mismatch: " + targetName + " is " + not + "an interface in " + this);
         }
      }

      abstract void validate(MixinInfo.State var1, List<ClassInfo> var2);

      abstract MixinPreProcessorStandard createPreProcessor(MixinInfo.MixinClassNode var1);

      static MixinInfo.SubType getTypeFor(MixinInfo mixin) {
         MixinInfo.Variant variant = MixinInfo.getVariant(mixin.getClassInfo());
         switch (variant) {
            case STANDARD:
               return new MixinInfo.SubType.Standard(mixin);
            case INTERFACE:
               return new MixinInfo.SubType.Interface(mixin);
            case ACCESSOR:
               return new MixinInfo.SubType.Accessor(mixin);
            default:
               throw new IllegalStateException("Unsupported Mixin variant " + variant + " for " + mixin);
         }
      }

      static class Accessor extends MixinInfo.SubType {
         private final Collection<String> interfaces = new ArrayList<>();

         Accessor(MixinInfo info) {
            super(info, "@Mixin", false);
            this.interfaces.add(info.getClassRef());
         }

         @Override
         boolean isLoadable() {
            return true;
         }

         @Override
         Collection<String> getInterfaces() {
            return this.interfaces;
         }

         @Override
         void validateTarget(String targetName, ClassInfo targetInfo) {
            boolean targetIsInterface = targetInfo.isInterface();
            if (targetIsInterface && !MixinEnvironment.getCompatibilityLevel().supports(1)) {
               throw new InvalidMixinException(this.mixin, "Accessor mixin targetting an interface is not supported in current enviromnment");
            }
         }

         @Override
         void validate(MixinInfo.State state, List<ClassInfo> targetClasses) {
            ClassNode classNode = state.getValidationClassNode();
            if (!"java/lang/Object".equals(classNode.superName)) {
               throw new InvalidMixinException(this.mixin, "Super class of " + this + " is invalid, found " + classNode.superName.replace('/', '.'));
            }
         }

         @Override
         MixinPreProcessorStandard createPreProcessor(MixinInfo.MixinClassNode classNode) {
            return new MixinPreProcessorAccessor(this.mixin, classNode);
         }
      }

      static class Interface extends MixinInfo.SubType {
         Interface(MixinInfo info) {
            super(info, "@Mixin", true);
         }

         @Override
         void validate(MixinInfo.State state, List<ClassInfo> targetClasses) {
            if (!MixinEnvironment.getCompatibilityLevel().supports(1)) {
               throw new InvalidMixinException(this.mixin, "Interface mixin not supported in current enviromnment");
            } else {
               ClassNode classNode = state.getValidationClassNode();
               if (!"java/lang/Object".equals(classNode.superName)) {
                  throw new InvalidMixinException(this.mixin, "Super class of " + this + " is invalid, found " + classNode.superName.replace('/', '.'));
               }
            }
         }

         @Override
         MixinPreProcessorStandard createPreProcessor(MixinInfo.MixinClassNode classNode) {
            return new MixinPreProcessorInterface(this.mixin, classNode);
         }
      }

      static class Standard extends MixinInfo.SubType {
         Standard(MixinInfo info) {
            super(info, "@Mixin", false);
         }

         @Override
         void validate(MixinInfo.State state, List<ClassInfo> targetClasses) {
            ClassNode classNode = state.getValidationClassNode();

            for (ClassInfo targetClass : targetClasses) {
               if (!classNode.superName.equals(targetClass.getSuperName())) {
                  if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.SUPER)) {
                     ClassInfo superClass = ClassInfo.forName(classNode.superName);
                     if (superClass.isMixin()) {
                        for (ClassInfo superTarget : superClass.getTargets()) {
                           if (targetClasses.contains(superTarget)) {
                              throw new InvalidMixinException(
                                 this.mixin,
                                 "Illegal hierarchy detected. Derived mixin "
                                    + this
                                    + " targets the same class "
                                    + superTarget.getClassName()
                                    + " as its superclass "
                                    + superClass.getClassName()
                              );
                           }
                        }
                     }

                     throw new InvalidMixinException(
                        this.mixin,
                        "Super class '"
                           + classNode.superName.replace('/', '.')
                           + "' of "
                           + this.mixin.getName()
                           + " was not found in the hierarchy of target class '"
                           + targetClass
                           + "'"
                     );
                  }

                  this.detached = true;
               }
            }
         }

         @Override
         MixinPreProcessorStandard createPreProcessor(MixinInfo.MixinClassNode classNode) {
            return new MixinPreProcessorStandard(this.mixin, classNode);
         }
      }
   }

   static enum Variant {
      STANDARD,
      INTERFACE,
      ACCESSOR,
      PROXY;

      private Variant() {
      }
   }
}
