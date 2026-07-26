package org.spongepowered.asm.mixin.injection.invoke;

import com.google.common.collect.ObjectArrays;
import com.google.common.primitives.Ints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;

public class RedirectInjector extends InvokeInjector {
   private static final String GET_CLASS_METHOD = "getClass";
   private static final String IS_ASSIGNABLE_FROM_METHOD = "isAssignableFrom";
   private static final String NPE = "java/lang/NullPointerException";
   private static final String KEY_NOMINATORS = "nominators";
   private static final String KEY_FUZZ = "fuzz";
   private static final String KEY_OPCODE = "opcode";
   protected RedirectInjector.Meta meta;
   private Map<BeforeNew, RedirectInjector.ConstructorRedirectData> ctorRedirectors = new HashMap<>();

   public RedirectInjector(InjectionInfo info) {
      this(info, "@Redirect");
   }

   protected RedirectInjector(InjectionInfo info, String annotationType) {
      super(info, annotationType);
      int priority = info.getContext().getPriority();
      boolean isFinal = Annotations.getVisible(this.methodNode, Final.class) != null;
      this.meta = new RedirectInjector.Meta(priority, isFinal, this.info.toString(), this.methodNode.desc);
   }

   @Override
   protected void checkTarget(Target target) {
   }

   @Override
   protected void addTargetNode(Target target, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode insn, Set<InjectionPoint> nominators) {
      InjectionNodes.InjectionNode node = target.getInjectionNode(insn);
      RedirectInjector.ConstructorRedirectData ctorData = null;
      int fuzz = 8;
      int opcode = 0;
      if (insn instanceof MethodInsnNode && "<init>".equals(((MethodInsnNode)insn).name)) {
         throw new InvalidInjectionException(this.info, String.format("Illegal %s of constructor specified on %s", this.annotationType, this));
      } else {
         if (node != null) {
            RedirectInjector.Meta other = node.getDecoration("redirector");
            if (other != null && other.getOwner() != this) {
               if (other.priority >= this.meta.priority) {
                  Injector.logger
                     .warn(
                        "{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                        new Object[]{this.annotationType, this.info, this.meta.priority, other.name, other.priority}
                     );
                  return;
               }

               if (other.isFinal) {
                  throw new InvalidInjectionException(
                     this.info, String.format("%s conflict: %s failed because target was already remapped by %s", this.annotationType, this, other.name)
                  );
               }
            }
         }

         for (InjectionPoint ip : nominators) {
            if (ip instanceof BeforeNew) {
               ctorData = this.getCtorRedirect((BeforeNew)ip);
               ctorData.wildcard = !((BeforeNew)ip).hasDescriptor();
            } else if (ip instanceof BeforeFieldAccess) {
               BeforeFieldAccess bfa = (BeforeFieldAccess)ip;
               fuzz = bfa.getFuzzFactor();
               opcode = bfa.getArrayOpcode();
            }
         }

         InjectionNodes.InjectionNode targetNode = target.addInjectionNode(insn);
         targetNode.decorate("redirector", this.meta);
         targetNode.decorate("nominators", nominators);
         if (insn instanceof TypeInsnNode && insn.getOpcode() == 187) {
            targetNode.decorate("ctor", ctorData);
         } else {
            targetNode.decorate("fuzz", fuzz);
            targetNode.decorate("opcode", opcode);
         }

         myNodes.add(targetNode);
      }
   }

   private RedirectInjector.ConstructorRedirectData getCtorRedirect(BeforeNew ip) {
      RedirectInjector.ConstructorRedirectData ctorRedirect = this.ctorRedirectors.get(ip);
      if (ctorRedirect == null) {
         ctorRedirect = new RedirectInjector.ConstructorRedirectData();
         this.ctorRedirectors.put(ip, ctorRedirect);
      }

      return ctorRedirect;
   }

   @Override
   protected void inject(Target target, InjectionNodes.InjectionNode node) {
      if (this.preInject(node)) {
         if (node.isReplaced()) {
            throw new UnsupportedOperationException("Redirector target failure for " + this.info);
         } else if (node.getCurrentTarget() instanceof MethodInsnNode) {
            this.checkTargetForNode(target, node, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);
            this.injectAtInvoke(target, node);
         } else if (node.getCurrentTarget() instanceof FieldInsnNode) {
            this.checkTargetForNode(target, node, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);
            this.injectAtFieldAccess(target, node);
         } else {
            if (node.getCurrentTarget() instanceof TypeInsnNode) {
               int opcode = node.getCurrentTarget().getOpcode();
               if (opcode == 187) {
                  if (!this.isStatic && target.isStatic) {
                     throw new InvalidInjectionException(
                        this.info, String.format("non-static callback method %s has a static target which is not supported", this)
                     );
                  }

                  this.injectAtConstructor(target, node);
                  return;
               }

               if (opcode == 193) {
                  this.checkTargetModifiers(target, false);
                  this.injectAtInstanceOf(target, node);
                  return;
               }
            }

            throw new InvalidInjectionException(
               this.info, String.format("%s annotation on is targetting an invalid insn in %s in %s", this.annotationType, target, this)
            );
         }
      }
   }

   protected boolean preInject(InjectionNodes.InjectionNode node) {
      RedirectInjector.Meta other = node.getDecoration("redirector");
      if (other.getOwner() != this) {
         Injector.logger
            .warn(
               "{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
               new Object[]{this.annotationType, this.info, this.meta.priority, other.name, other.priority}
            );
         return false;
      } else {
         return true;
      }
   }

   @Override
   protected void postInject(Target target, InjectionNodes.InjectionNode node) {
      super.postInject(target, node);
      if (node.getOriginalTarget() instanceof TypeInsnNode && node.getOriginalTarget().getOpcode() == 187) {
         RedirectInjector.ConstructorRedirectData meta = node.getDecoration("ctor");
         if (meta.wildcard && meta.injected == 0) {
            throw new InvalidInjectionException(
               this.info, String.format("%s ctor invocation was not found in %s", this.annotationType, target), meta.lastException
            );
         }
      }
   }

   @Override
   protected void injectAtInvoke(Target target, InjectionNodes.InjectionNode node) {
      RedirectInjector.RedirectedInvokeData invoke = new RedirectInjector.RedirectedInvokeData(target, (MethodInsnNode)node.getCurrentTarget());
      this.validateParams(invoke, invoke.returnType, invoke.handlerArgs);
      InsnList insns = new InsnList();
      Target.Extension extraLocals = target.extendLocals().add(invoke.handlerArgs).add(1);
      Target.Extension extraStack = target.extendStack().add(1);
      int[] argMap = this.storeArgs(target, invoke.handlerArgs, insns, 0);
      if (invoke.captureTargetArgs > 0) {
         int argSize = Bytecode.getArgsSize(target.arguments, 0, invoke.captureTargetArgs);
         extraLocals.add(argSize);
         extraStack.add(argSize);
         argMap = Ints.concat(new int[][]{argMap, target.getArgIndices()});
      }

      AbstractInsnNode champion = this.invokeHandlerWithArgs(this.methodArgs, insns, argMap);
      if (invoke.coerceReturnType && invoke.returnType.getSort() >= 9) {
         insns.add(new TypeInsnNode(192, invoke.returnType.getInternalName()));
      }

      target.replaceNode(invoke.node, champion, insns);
      extraLocals.apply();
      extraStack.apply();
   }

   private void injectAtFieldAccess(Target target, InjectionNodes.InjectionNode node) {
      RedirectInjector.RedirectedFieldData field = new RedirectInjector.RedirectedFieldData(target, (FieldInsnNode)node.getCurrentTarget());
      int handlerDimensions = this.returnType.getSort() == 9 ? this.returnType.getDimensions() : 0;
      if (handlerDimensions > field.dimensions) {
         throw new InvalidInjectionException(this.info, "Dimensionality of handler method is greater than target array on " + this);
      } else {
         if (handlerDimensions == 0 && field.dimensions > 0) {
            int fuzz = node.<Integer>getDecoration("fuzz");
            int opcode = node.<Integer>getDecoration("opcode");
            this.injectAtArrayField(field, fuzz, opcode);
         } else {
            this.injectAtScalarField(field);
         }
      }
   }

   private void injectAtArrayField(RedirectInjector.RedirectedFieldData field, int fuzz, int opcode) {
      Type elementType = field.type.getElementType();
      if (field.opcode != 178 && field.opcode != 180) {
         throw new InvalidInjectionException(
            this.info, String.format("Unspported opcode %s for array access %s", Bytecode.getOpcodeName(field.opcode), this.info)
         );
      } else {
         if (this.returnType.getSort() != 0) {
            if (opcode != 190) {
               opcode = elementType.getOpcode(46);
            }

            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(field.target.insns, field.node, opcode, fuzz);
            this.injectAtGetArray(field, varNode);
         } else {
            AbstractInsnNode varNode = BeforeFieldAccess.findArrayNode(field.target.insns, field.node, elementType.getOpcode(79), fuzz);
            this.injectAtSetArray(field, varNode);
         }
      }
   }

   private void injectAtGetArray(RedirectInjector.RedirectedFieldData field, AbstractInsnNode varNode) {
      field.description = "array getter";
      field.elementType = field.type.getElementType();
      if (varNode != null && varNode.getOpcode() == 190) {
         field.elementType = Type.INT_TYPE;
         field.extraDimensions = 0;
      }

      this.validateParams(field, field.elementType, field.getArrayArgs());
      this.injectArrayRedirect(field, varNode, "array getter");
   }

   private void injectAtSetArray(RedirectInjector.RedirectedFieldData field, AbstractInsnNode varNode) {
      field.description = "array setter";
      Type elementType = field.type.getElementType();
      int valueArgIndex = field.getTotalDimensions();
      if (this.checkCoerce(
         valueArgIndex, elementType, String.format("%s array setter method %s from %s", this.annotationType, this, this.info.getContext()), true
      )) {
         elementType = this.methodArgs[valueArgIndex];
      }

      this.validateParams(field, Type.VOID_TYPE, field.getArrayArgs(elementType));
      this.injectArrayRedirect(field, varNode, "array setter");
   }

   private void injectArrayRedirect(RedirectInjector.RedirectedFieldData field, AbstractInsnNode varNode, String type) {
      if (varNode == null) {
         String advice = "";
         throw new InvalidInjectionException(
            this.info,
            String.format("Array element %s on %s could not locate a matching %s instruction in %s. %s", this.annotationType, this, type, field.target, advice)
         );
      } else {
         Target.Extension extraStack = field.target.extendStack();
         if (!this.isStatic) {
            VarInsnNode loadThis = new VarInsnNode(25, 0);
            field.target.insns.insert(field.node, loadThis);
            field.target.insns.insert(loadThis, new InsnNode(95));
            extraStack.add();
         }

         InsnList insns = new InsnList();
         if (field.captureTargetArgs > 0) {
            this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
         }

         extraStack.apply();
         AbstractInsnNode champion = this.invokeHandler(insns);
         if (field.coerceReturnType && field.type.getSort() >= 9) {
            insns.add(new TypeInsnNode(192, field.elementType.getInternalName()));
         }

         field.target.replaceNode(varNode, champion, insns);
      }
   }

   private void injectAtScalarField(RedirectInjector.RedirectedFieldData field) {
      AbstractInsnNode invoke = null;
      InsnList insns = new InsnList();
      if (field.isGetter) {
         invoke = this.injectAtGetField(field, insns);
      } else {
         if (!field.isSetter) {
            throw new InvalidInjectionException(this.info, String.format("Unspported opcode %s for %s", Bytecode.getOpcodeName(field.opcode), this.info));
         }

         invoke = this.injectAtPutField(field, insns);
      }

      field.target.replaceNode(field.node, invoke, insns);
   }

   private AbstractInsnNode injectAtGetField(RedirectInjector.RedirectedFieldData field, InsnList insns) {
      this.validateParams(field, field.type, new Type[]{field.isStatic ? null : field.owner});
      Target.Extension extraStack = field.target.extendStack();
      if (!this.isStatic) {
         extraStack.add();
         insns.add(new VarInsnNode(25, 0));
         if (!field.isStatic) {
            insns.add(new InsnNode(95));
         }
      }

      if (field.captureTargetArgs > 0) {
         this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
      }

      extraStack.apply();
      AbstractInsnNode champion = this.invokeHandler(insns);
      if (field.coerceReturnType && field.type.getSort() >= 9) {
         insns.add(new TypeInsnNode(192, field.type.getInternalName()));
      }

      return champion;
   }

   private AbstractInsnNode injectAtPutField(RedirectInjector.RedirectedFieldData field, InsnList insns) {
      this.validateParams(field, Type.VOID_TYPE, new Type[]{field.isStatic ? null : field.owner, field.type});
      Target.Extension extraStack = field.target.extendStack();
      if (!this.isStatic) {
         if (field.isStatic) {
            insns.add(new VarInsnNode(25, 0));
            insns.add(new InsnNode(95));
         } else {
            extraStack.add();
            int marshallVar = field.target.allocateLocals(field.type.getSize());
            insns.add(new VarInsnNode(field.type.getOpcode(54), marshallVar));
            insns.add(new VarInsnNode(25, 0));
            insns.add(new InsnNode(95));
            insns.add(new VarInsnNode(field.type.getOpcode(21), marshallVar));
         }
      }

      if (field.captureTargetArgs > 0) {
         this.pushArgs(field.target.arguments, insns, field.target.getArgIndices(), 0, field.captureTargetArgs, extraStack);
      }

      extraStack.apply();
      return this.invokeHandler(insns);
   }

   protected void injectAtConstructor(Target target, InjectionNodes.InjectionNode node) {
      RedirectInjector.ConstructorRedirectData meta = node.getDecoration("ctor");
      if (meta == null) {
         throw new InvalidInjectionException(
            this.info, String.format("%s ctor redirector has no metadata, the injector failed a preprocessing phase", this.annotationType)
         );
      } else {
         TypeInsnNode newNode = (TypeInsnNode)node.getCurrentTarget();
         AbstractInsnNode dupNode = target.get(target.indexOf(newNode) + 1);
         MethodInsnNode initNode = target.findInitNodeFor(newNode);
         if (initNode == null) {
            meta.throwOrCollect(new InvalidInjectionException(this.info, String.format("%s ctor invocation was not found in %s", this.annotationType, target)));
         } else {
            boolean isAssigned = dupNode.getOpcode() == 89;
            RedirectInjector.RedirectedInvokeData ctor = new RedirectInjector.RedirectedInvokeData(target, initNode);
            ctor.description = "factory";

            try {
               this.validateParams(ctor, Type.getObjectType(newNode.desc), ctor.targetArgs);
            } catch (InvalidInjectionException var11) {
               meta.throwOrCollect(var11);
               return;
            }

            if (isAssigned) {
               target.removeNode(dupNode);
            }

            if (this.isStatic) {
               target.removeNode(newNode);
            } else {
               target.replaceNode(newNode, new VarInsnNode(25, 0));
            }

            Target.Extension extraStack = target.extendStack();
            InsnList insns = new InsnList();
            if (ctor.captureTargetArgs > 0) {
               this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, ctor.captureTargetArgs, extraStack);
            }

            this.invokeHandler(insns);
            if (ctor.coerceReturnType) {
               insns.add(new TypeInsnNode(192, newNode.desc));
            }

            extraStack.apply();
            if (isAssigned) {
               this.doNullCheck(insns, extraStack, "constructor handler", newNode.desc.replace('/', '.'));
            } else {
               insns.add(new InsnNode(87));
            }

            extraStack.apply();
            target.replaceNode(initNode, insns);
            meta.injected++;
         }
      }
   }

   protected void injectAtInstanceOf(Target target, InjectionNodes.InjectionNode node) {
      this.injectAtInstanceOf(target, (TypeInsnNode)node.getCurrentTarget());
   }

   protected void injectAtInstanceOf(Target target, TypeInsnNode typeNode) {
      if (this.returnType.getSort() == 1) {
         this.redirectInstanceOf(target, typeNode, false);
      } else if (this.returnType.equals(Type.getType("Ljava/lang/Class;"))) {
         this.redirectInstanceOf(target, typeNode, true);
      } else {
         throw new InvalidInjectionException(
            this.info,
            String.format(
               "%s on %s has an invalid signature. Found unexpected return type %s. INSTANCEOF handler expects (Ljava/lang/Object;Ljava/lang/Class;)Z or (Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Class;",
               this.annotationType,
               this,
               SignaturePrinter.getTypeName(this.returnType)
            )
         );
      }
   }

   private void redirectInstanceOf(Target target, TypeInsnNode typeNode, boolean dynamic) {
      Target.Extension extraStack = target.extendStack();
      InsnList insns = new InsnList();
      Injector.InjectorData handler = new Injector.InjectorData(target, "instanceof handler", false);
      this.validateParams(handler, this.returnType, new Type[]{Type.getType("Ljava/lang/Object;"), Type.getType("Ljava/lang/Class;")});
      if (dynamic) {
         insns.add(new InsnNode(89));
         extraStack.add();
      }

      if (!this.isStatic) {
         insns.add(new VarInsnNode(25, 0));
         insns.add(new InsnNode(95));
         extraStack.add();
      }

      insns.add(new LdcInsnNode(Type.getObjectType(typeNode.desc)));
      extraStack.add();
      if (handler.captureTargetArgs > 0) {
         this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
      }

      AbstractInsnNode champion = this.invokeHandler(insns);
      if (dynamic) {
         this.doNullCheck(insns, extraStack, "instanceof handler", "class type");
         this.checkIsAssignableFrom(insns, extraStack);
      }

      target.replaceNode(typeNode, champion, insns);
      extraStack.apply();
   }

   private void checkIsAssignableFrom(InsnList insns, Target.Extension extraStack) {
      LabelNode objectIsNull = new LabelNode();
      LabelNode checkComplete = new LabelNode();
      insns.add(new InsnNode(95));
      insns.add(new InsnNode(89));
      extraStack.add();
      insns.add(new JumpInsnNode(198, objectIsNull));
      insns.add(new MethodInsnNode(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false));
      insns.add(new MethodInsnNode(182, "java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z", false));
      insns.add(new JumpInsnNode(167, checkComplete));
      insns.add(objectIsNull);
      insns.add(new InsnNode(87));
      insns.add(new InsnNode(87));
      insns.add(new InsnNode(3));
      insns.add(checkComplete);
      extraStack.add();
   }

   private void doNullCheck(InsnList insns, Target.Extension extraStack, String type, String value) {
      LabelNode nullCheckSucceeded = new LabelNode();
      insns.add(new InsnNode(89));
      insns.add(new JumpInsnNode(199, nullCheckSucceeded));
      this.throwException(insns, "java/lang/NullPointerException", String.format("%s %s %s returned null for %s", this.annotationType, type, this, value));
      insns.add(nullCheckSucceeded);
      extraStack.add();
   }

   static class ConstructorRedirectData {
      public static final String KEY = "ctor";
      boolean wildcard = false;
      int injected = 0;
      InvalidInjectionException lastException;

      ConstructorRedirectData() {
      }

      public void throwOrCollect(InvalidInjectionException ex) {
         if (!this.wildcard) {
            throw ex;
         } else {
            this.lastException = ex;
         }
      }
   }

   class Meta {
      public static final String KEY = "redirector";
      final int priority;
      final boolean isFinal;
      final String name;
      final String desc;

      public Meta(int priority, boolean isFinal, String name, String desc) {
         this.priority = priority;
         this.isFinal = isFinal;
         this.name = name;
         this.desc = desc;
      }

      RedirectInjector getOwner() {
         return RedirectInjector.this;
      }
   }

   static class RedirectedFieldData extends Injector.InjectorData {
      final FieldInsnNode node;
      final int opcode;
      final Type owner;
      final Type type;
      final int dimensions;
      final boolean isStatic;
      final boolean isGetter;
      final boolean isSetter;
      Type elementType;
      int extraDimensions = 1;

      RedirectedFieldData(Target target, FieldInsnNode node) {
         super(target);
         this.node = node;
         this.opcode = node.getOpcode();
         this.owner = Type.getObjectType(node.owner);
         this.type = Type.getType(node.desc);
         this.dimensions = this.type.getSort() == 9 ? this.type.getDimensions() : 0;
         this.isStatic = this.opcode == 178 || this.opcode == 179;
         this.isGetter = this.opcode == 178 || this.opcode == 180;
         this.isSetter = this.opcode == 179 || this.opcode == 181;
         this.description = this.isGetter ? "field getter" : (this.isSetter ? "field setter" : "handler");
      }

      int getTotalDimensions() {
         return this.dimensions + this.extraDimensions;
      }

      Type[] getArrayArgs(Type... extra) {
         int dimensions = this.getTotalDimensions();
         Type[] args = new Type[dimensions + extra.length];

         for (int i = 0; i < args.length; i++) {
            args[i] = i == 0 ? this.type : (i < dimensions ? Type.INT_TYPE : extra[dimensions - i]);
         }

         return args;
      }
   }

   static class RedirectedInvokeData extends Injector.InjectorData {
      final MethodInsnNode node;
      final Type returnType;
      final Type[] targetArgs;
      final Type[] handlerArgs;

      RedirectedInvokeData(Target target, MethodInsnNode node) {
         super(target);
         this.node = node;
         this.returnType = Type.getReturnType(node.desc);
         this.targetArgs = Type.getArgumentTypes(node.desc);
         this.handlerArgs = node.getOpcode() == 184 ? this.targetArgs : (Type[])ObjectArrays.concat(Type.getObjectType(node.owner), this.targetArgs);
      }
   }
}
