package org.spongepowered.asm.util.asm;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class ElementNode<TNode> {
   private final ClassNode owner;

   protected ElementNode(ClassNode owner) {
      this.owner = owner;
   }

   public boolean isMethod() {
      return false;
   }

   public boolean isField() {
      return false;
   }

   public MethodNode getMethod() {
      return null;
   }

   public FieldNode getField() {
      return null;
   }

   public ClassNode getOwner() {
      return this.owner;
   }

   public String getOwnerName() {
      return this.owner != null ? this.owner.name : null;
   }

   public abstract String getName();

   public abstract String getDesc();

   public abstract String getSignature();

   public abstract TNode get();

   public static ElementNode<MethodNode> of(ClassNode owner, MethodNode method) {
      return new ElementNode.ElementNodeMethod(owner, method);
   }

   public static ElementNode<FieldNode> of(ClassNode owner, FieldNode field) {
      return new ElementNode.ElementNodeField(owner, field);
   }

   public static <TNode> ElementNode<TNode> of(ClassNode owner, TNode node) {
      if (node instanceof MethodNode) {
         return new ElementNode.ElementNodeMethod(owner, (MethodNode)node);
      } else if (node instanceof FieldNode) {
         return new ElementNode.ElementNodeField(owner, (FieldNode)node);
      } else {
         throw new IllegalArgumentException("Could not create ElementNode for unknown node type: " + node.getClass().getName());
      }
   }

   public static <TNode> List<ElementNode<TNode>> listOf(ClassNode owner, List<TNode> list) {
      List<ElementNode<TNode>> nodes = new ArrayList<>();

      for (TNode node : list) {
         nodes.add(of(owner, node));
      }

      return nodes;
   }

   public static List<ElementNode<FieldNode>> fieldList(ClassNode owner) {
      List<ElementNode<FieldNode>> fields = new ArrayList<>();

      for (FieldNode field : owner.fields) {
         fields.add(new ElementNode.ElementNodeField(owner, field));
      }

      return fields;
   }

   public static List<ElementNode<MethodNode>> methodList(ClassNode owner) {
      List<ElementNode<MethodNode>> methods = new ArrayList<>();

      for (MethodNode method : owner.methods) {
         methods.add(new ElementNode.ElementNodeMethod(owner, method));
      }

      return methods;
   }

   static class ElementNodeField extends ElementNode<FieldNode> {
      private FieldNode field;

      ElementNodeField(ClassNode owner, FieldNode field) {
         super(owner);
         this.field = field;
      }

      @Override
      public boolean isField() {
         return true;
      }

      @Override
      public FieldNode getField() {
         return this.field;
      }

      @Override
      public String getName() {
         return this.field.name;
      }

      @Override
      public String getDesc() {
         return this.field.desc;
      }

      @Override
      public String getSignature() {
         return this.field.signature;
      }

      public FieldNode get() {
         return this.field;
      }

      @Override
      public String toString() {
         return String.format("FieldElement[%s:%s]", this.field.name, this.field.desc);
      }
   }

   static class ElementNodeMethod extends ElementNode<MethodNode> {
      private MethodNode method;

      ElementNodeMethod(ClassNode owner, MethodNode method) {
         super(owner);
         this.method = method;
      }

      @Override
      public boolean isMethod() {
         return true;
      }

      @Override
      public MethodNode getMethod() {
         return this.method;
      }

      @Override
      public String getName() {
         return this.method.name;
      }

      @Override
      public String getDesc() {
         return this.method.desc;
      }

      @Override
      public String getSignature() {
         return this.method.signature;
      }

      public MethodNode get() {
         return this.method;
      }

      @Override
      public String toString() {
         return String.format("MethodElement[%s%s]", this.method.name, this.method.desc);
      }
   }
}
