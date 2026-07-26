package org.spongepowered.asm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MixinVerifier;
import org.spongepowered.asm.util.throwables.LVTGeneratorError;

public final class Locals {
   private static final String[] FRAME_TYPES = new String[]{"TOP", "INTEGER", "FLOAT", "DOUBLE", "LONG", "NULL", "UNINITIALIZED_THIS"};
   private static final Map<String, List<LocalVariableNode>> calculatedLocalVariables = new HashMap<>();

   private Locals() {
   }

   public static void loadLocals(Type[] locals, InsnList insns, int pos, int limit) {
      while (pos < locals.length && limit > 0) {
         if (locals[pos] != null) {
            insns.add(new VarInsnNode(locals[pos].getOpcode(21), pos));
            limit--;
         }

         pos++;
      }
   }

   public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node) {
      for (int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); i++) {
         node = nextNode(method.instructions, node);
      }

      ClassInfo classInfo = ClassInfo.forName(classNode.name);
      if (classInfo == null) {
         throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
      } else {
         ClassInfo.Method methodInfo = classInfo.findMethod(method, method.access | 262144);
         if (methodInfo == null) {
            throw new LVTGeneratorError("Could not locate method metadata for " + method.name + " generating LVT in " + classNode.name);
         } else {
            List<ClassInfo.FrameData> frames = methodInfo.getFrames();
            LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
            int local = 0;
            int index = 0;
            if ((method.access & 8) == 0) {
               frame[local++] = new LocalVariableNode("this", Type.getObjectType(classNode.name).toString(), null, null, null, 0);
            }

            for (Type argType : Type.getArgumentTypes(method.desc)) {
               frame[local] = new LocalVariableNode("arg" + index++, argType.toString(), null, null, null, local);
               local += argType.getSize();
            }

            int initialFrameSize = local;
            int frameSize = local;
            int frameIndex = -1;
            int lastFrameSize = local;
            VarInsnNode storeInsn = null;

            for (AbstractInsnNode insn : method.instructions) {
               if (storeInsn != null) {
                  frame[storeInsn.var] = getLocalVariableAt(classNode, method, insn, storeInsn.var);
                  storeInsn = null;
               }

               if (insn instanceof FrameNode) {
                  frameIndex++;
                  FrameNode frameNode = (FrameNode)insn;
                  if (frameNode.type != 3 && frameNode.type != 4) {
                     ClassInfo.FrameData frameData = frameIndex < frames.size() ? frames.get(frameIndex) : null;
                     if (frameData != null) {
                        if (frameData.type == 0) {
                           frameSize = Math.min(frameSize, frameData.locals);
                           lastFrameSize = frameSize;
                        } else {
                           frameSize = getAdjustedFrameSize(frameSize, frameData);
                        }
                     } else {
                        frameSize = getAdjustedFrameSize(frameSize, frameNode);
                     }

                     if (frameNode.type == 2) {
                        for (int framePos = frameSize; framePos < frame.length; framePos++) {
                           frame[framePos] = null;
                        }

                        lastFrameSize = frameSize;
                     } else {
                        int framePos = frameNode.type == 1 ? lastFrameSize : 0;
                        lastFrameSize = frameSize;

                        for (int localPos = 0; framePos < frame.length; localPos++) {
                           Object localType = localPos < frameNode.local.size() ? frameNode.local.get(localPos) : null;
                           if (localType instanceof String) {
                              frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                           } else if (localType instanceof Integer) {
                              boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL;
                              boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                              boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                              if (localType != Opcodes.TOP) {
                                 if (isMarkerType) {
                                    frame[framePos] = null;
                                 } else {
                                    if (!is32bitValue && !is64bitValue) {
                                       throw new LVTGeneratorError(
                                          "Unrecognised locals opcode "
                                             + localType
                                             + " in locals array at position "
                                             + localPos
                                             + " in "
                                             + classNode.name
                                             + "."
                                             + method.name
                                             + method.desc
                                       );
                                    }

                                    frame[framePos] = getLocalVariableAt(classNode, method, insn, framePos);
                                    if (is64bitValue) {
                                       framePos++;
                                       frame[framePos] = null;
                                    }
                                 }
                              }
                           } else if (localType == null) {
                              if (framePos >= initialFrameSize && framePos >= frameSize && frameSize > 0) {
                                 frame[framePos] = null;
                              }
                           } else if (!(localType instanceof LabelNode)) {
                              throw new LVTGeneratorError(
                                 "Invalid value "
                                    + localType
                                    + " in locals array at position "
                                    + localPos
                                    + " in "
                                    + classNode.name
                                    + "."
                                    + method.name
                                    + method.desc
                              );
                           }

                           framePos++;
                        }
                     }
                  }
               } else if (insn instanceof VarInsnNode) {
                  VarInsnNode varNode = (VarInsnNode)insn;
                  boolean isLoad = insn.getOpcode() >= 21 && insn.getOpcode() <= 53;
                  if (isLoad) {
                     frame[varNode.var] = getLocalVariableAt(classNode, method, insn, varNode.var);
                  } else {
                     storeInsn = varNode;
                  }
               }

               if (insn == node) {
                  break;
               }
            }

            for (int l = 0; l < frame.length; l++) {
               if (frame[l] != null && frame[l].desc == null) {
                  frame[l] = null;
               }
            }

            return frame;
         }
      }
   }

   public static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, AbstractInsnNode node, int var) {
      return getLocalVariableAt(classNode, method, method.instructions.indexOf(node), var);
   }

   private static LocalVariableNode getLocalVariableAt(ClassNode classNode, MethodNode method, int pos, int var) {
      LocalVariableNode localVariableNode = null;
      LocalVariableNode fallbackNode = null;

      for (LocalVariableNode local : getLocalVariableTable(classNode, method)) {
         if (local.index == var) {
            if (isOpcodeInRange(method.instructions, local, pos)) {
               localVariableNode = local;
            } else if (localVariableNode == null) {
               fallbackNode = local;
            }
         }
      }

      if (localVariableNode == null && !method.localVariables.isEmpty()) {
         for (LocalVariableNode localx : getGeneratedLocalVariableTable(classNode, method)) {
            if (localx.index == var && isOpcodeInRange(method.instructions, localx, pos)) {
               localVariableNode = localx;
            }
         }
      }

      return localVariableNode != null ? localVariableNode : fallbackNode;
   }

   private static boolean isOpcodeInRange(InsnList insns, LocalVariableNode local, int pos) {
      return insns.indexOf(local.start) <= pos && insns.indexOf(local.end) > pos;
   }

   public static List<LocalVariableNode> getLocalVariableTable(ClassNode classNode, MethodNode method) {
      return method.localVariables.isEmpty() ? getGeneratedLocalVariableTable(classNode, method) : method.localVariables;
   }

   public static List<LocalVariableNode> getGeneratedLocalVariableTable(ClassNode classNode, MethodNode method) {
      String methodId = String.format("%s.%s%s", classNode.name, method.name, method.desc);
      List<LocalVariableNode> localVars = calculatedLocalVariables.get(methodId);
      if (localVars != null) {
         return localVars;
      } else {
         localVars = generateLocalVariableTable(classNode, method);
         calculatedLocalVariables.put(methodId, localVars);
         return localVars;
      }
   }

   public static List<LocalVariableNode> generateLocalVariableTable(ClassNode classNode, MethodNode method) {
      List<Type> interfaces = null;
      if (classNode.interfaces != null) {
         interfaces = new ArrayList<>();

         for (String iface : classNode.interfaces) {
            interfaces.add(Type.getObjectType(iface));
         }
      }

      Type objectType = null;
      if (classNode.superName != null) {
         objectType = Type.getObjectType(classNode.superName);
      }

      Analyzer<BasicValue> analyzer = new Analyzer(new MixinVerifier(ASM.API_VERSION, Type.getObjectType(classNode.name), objectType, interfaces, false));

      try {
         analyzer.analyze(classNode.name, method);
      } catch (AnalyzerException var18) {
         var18.printStackTrace();
      }

      Frame<BasicValue>[] frames = analyzer.getFrames();
      int methodSize = method.instructions.size();
      List<LocalVariableNode> localVariables = new ArrayList<>();
      LocalVariableNode[] localNodes = new LocalVariableNode[method.maxLocals];
      BasicValue[] locals = new BasicValue[method.maxLocals];
      LabelNode[] labels = new LabelNode[methodSize];
      String[] lastKnownType = new String[method.maxLocals];

      for (int i = 0; i < methodSize; i++) {
         Frame<BasicValue> f = frames[i];
         if (f != null) {
            LabelNode label = null;

            for (int j = 0; j < f.getLocals(); j++) {
               BasicValue local = (BasicValue)f.getLocal(j);
               if ((local != null || locals[j] != null) && (local == null || !local.equals(locals[j]))) {
                  if (label == null) {
                     AbstractInsnNode existingLabel = method.instructions.get(i);
                     if (existingLabel instanceof LabelNode) {
                        label = (LabelNode)existingLabel;
                     } else {
                        labels[i] = label = new LabelNode();
                     }
                  }

                  if (local == null && locals[j] != null) {
                     localVariables.add(localNodes[j]);
                     localNodes[j].end = label;
                     localNodes[j] = null;
                  } else if (local != null) {
                     if (locals[j] != null) {
                        localVariables.add(localNodes[j]);
                        localNodes[j].end = label;
                        localNodes[j] = null;
                     }

                     String desc = local.getType() != null ? local.getType().getDescriptor() : lastKnownType[j];
                     localNodes[j] = new LocalVariableNode("var" + j, desc, null, label, null, j);
                     if (desc != null) {
                        lastKnownType[j] = desc;
                     }
                  }

                  locals[j] = local;
               }
            }
         }
      }

      LabelNode label = null;

      for (int k = 0; k < localNodes.length; k++) {
         if (localNodes[k] != null) {
            if (label == null) {
               label = new LabelNode();
               method.instructions.add(label);
            }

            localNodes[k].end = label;
            localVariables.add(localNodes[k]);
         }
      }

      for (int n = methodSize - 1; n >= 0; n--) {
         if (labels[n] != null) {
            method.instructions.insert(method.instructions.get(n), labels[n]);
         }
      }

      return localVariables;
   }

   private static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
      int index = insns.indexOf(insn) + 1;
      return index > 0 && index < insns.size() ? insns.get(index) : insn;
   }

   private static int getAdjustedFrameSize(int currentSize, FrameNode frameNode) {
      return getAdjustedFrameSize(currentSize, frameNode.type, computeFrameSize(frameNode));
   }

   private static int getAdjustedFrameSize(int currentSize, ClassInfo.FrameData frameData) {
      return getAdjustedFrameSize(currentSize, frameData.type, frameData.size);
   }

   private static int getAdjustedFrameSize(int currentSize, int type, int size) {
      switch (type) {
         case -1:
         case 0:
            return size;
         case 1:
            return currentSize + size;
         case 2:
            return currentSize - size;
         case 3:
         case 4:
            return currentSize;
         default:
            return currentSize;
      }
   }

   public static int computeFrameSize(FrameNode frameNode) {
      if (frameNode.local == null) {
         return 0;
      } else {
         int size = 0;

         for (Object local : frameNode.local) {
            if (local instanceof Integer) {
               size += local != Opcodes.DOUBLE && local != Opcodes.LONG ? 1 : 2;
            } else {
               size++;
            }
         }

         return size;
      }
   }

   public static String getFrameTypeName(Object frameEntry) {
      if (frameEntry == null) {
         return "NULL";
      } else if (frameEntry instanceof String) {
         return Bytecode.getSimpleName(frameEntry.toString());
      } else if (frameEntry instanceof Integer) {
         int type = (Integer)frameEntry;
         return type >= FRAME_TYPES.length ? "INVALID" : FRAME_TYPES[type];
      } else {
         return "?";
      }
   }
}
