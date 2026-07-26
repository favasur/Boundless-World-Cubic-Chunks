package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode("INVOKE_ASSIGN")
public class AfterInvoke extends BeforeInvoke {
   public AfterInvoke(InjectionPointData data) {
      super(data);
   }

   @Override
   protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
      MethodInsnNode methodNode = (MethodInsnNode)insn;
      if (Type.getReturnType(methodNode.desc) == Type.VOID_TYPE) {
         return false;
      } else {
         insn = InjectionPoint.nextNode(insns, insn);
         if (insn instanceof VarInsnNode && insn.getOpcode() >= 54) {
            insn = InjectionPoint.nextNode(insns, insn);
         }

         nodes.add(insn);
         return true;
      }
   }
}
