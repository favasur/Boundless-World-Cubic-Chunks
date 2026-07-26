package org.spongepowered.asm.util.asm;

import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public class MixinVerifier extends SimpleVerifier {
   private Type currentClass;
   private Type currentSuperClass;
   private List<Type> currentClassInterfaces;
   private boolean isInterface;

   public MixinVerifier(int api, Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface) {
      super(api, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
      this.currentClass = currentClass;
      this.currentSuperClass = currentSuperClass;
      this.currentClassInterfaces = currentClassInterfaces;
      this.isInterface = isInterface;
   }

   protected boolean isInterface(Type type) {
      return this.currentClass != null && type.equals(this.currentClass)
         ? this.isInterface
         : ClassInfo.forType(type, ClassInfo.TypeLookup.ELEMENT_TYPE).isInterface();
   }

   protected Type getSuperClass(Type type) {
      if (this.currentClass != null && type.equals(this.currentClass)) {
         return this.currentSuperClass;
      } else {
         ClassInfo c = ClassInfo.forType(type, ClassInfo.TypeLookup.ELEMENT_TYPE).getSuperClass();
         return c == null ? null : Type.getType("L" + c.getName() + ";");
      }
   }

   protected boolean isAssignableFrom(Type type, Type other) {
      if (type.equals(other)) {
         return true;
      } else if (this.currentClass != null && type.equals(this.currentClass)) {
         if (this.getSuperClass(other) == null) {
            return false;
         } else {
            return !this.isInterface ? this.isAssignableFrom(type, this.getSuperClass(other)) : other.getSort() == 10 || other.getSort() == 9;
         }
      } else if (this.currentClass == null || !other.equals(this.currentClass)) {
         ClassInfo typeInfo = ClassInfo.forType(type, ClassInfo.TypeLookup.ELEMENT_TYPE);
         if (typeInfo == null) {
            return false;
         } else {
            if (typeInfo.isInterface()) {
               typeInfo = ClassInfo.forName("java/lang/Object");
            }

            return ClassInfo.forType(other, ClassInfo.TypeLookup.ELEMENT_TYPE).hasSuperClass(typeInfo);
         }
      } else if (this.isAssignableFrom(type, this.currentSuperClass)) {
         return true;
      } else {
         if (this.currentClassInterfaces != null) {
            for (int i = 0; i < this.currentClassInterfaces.size(); i++) {
               Type v = this.currentClassInterfaces.get(i);
               if (this.isAssignableFrom(type, v)) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
