package io.github.opencubicchunks.cubicchunks.core.asm.transformer;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class CubicChunksWorldEditTransformer implements IClassTransformer {
   public CubicChunksWorldEditTransformer() {
   }

   public byte[] transform(String name, String transformedName, byte[] basicClass) {
      if (!"com.sk89q.worldedit.forge.ForgeWorld".equals(transformedName)) {
         return basicClass;
      } else {
         ClassReader cr = new ClassReader(basicClass);
         ClassNode node = new ClassNode();
         cr.accept(node, 0);

         for (MethodNode method : node.methods) {
            if (method.name.equals("getMinY") && method.desc.equals("()I")) {
               this.transformGetMinY(method);
            }
         }

         ClassWriter cw = new ClassWriter(0);
         node.accept(cw);
         return cw.toByteArray();
      }
   }

   private byte[] unrelocate(byte[] basicClass) {
      Remapper remapper = new Remapper() {
         public String map(String typeName) {
            return typeName.replace("io/github/opencubicchunks/cubicchunks/cubicgen/blue/endless", "blue/endless");
         }
      };
      ClassWriter cw = new ClassWriter(0);
      ClassRemapper classRemapper = new ClassRemapper(cw, remapper);
      ClassReader classReader = new ClassReader(basicClass);
      classReader.accept(classRemapper, 0);
      return cw.toByteArray();
   }

   private void transformGetMinY(MethodNode getMinY) {
      InsnList list = getMinY.instructions;
      list.clear();
      LabelNode start = new LabelNode(new Label());
      LabelNode end = new LabelNode(new Label());
      list.add(start);
      list.add(new LineNumberNode(10000, start));
      list.add(new IntInsnNode(25, 0));
      list.add(new MethodInsnNode(182, "com/sk89q/worldedit/forge/ForgeWorld", "getWorld", "()Lnet/minecraft/world/World;", false));
      list.add(new TypeInsnNode(192, "io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld"));
      list.add(new MethodInsnNode(185, "io/github/opencubicchunks/cubicchunks/api/world/ICubicWorld", "getMinHeight", "()I", true));
      list.add(new InsnNode(172));
      list.add(end);
      getMinY.localVariables.clear();
      getMinY.localVariables.add(new LocalVariableNode("this", "Lcom/sk89q/worldedit/forge/ForgeWorld;", null, start, end, 0));
      getMinY.maxLocals = 1;
      getMinY.maxStack = 1;
   }

   public int getMinY() {
      return 0;
   }
}
