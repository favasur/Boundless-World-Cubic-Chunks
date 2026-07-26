package io.github.opencubicchunks.cubicchunks.api.util;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Box {
   protected int x1;
   protected int y1;
   protected int z1;
   protected int x2;
   protected int y2;
   protected int z2;

   public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
      this.x1 = Math.min(x1, x2);
      this.y1 = Math.min(y1, y2);
      this.z1 = Math.min(z1, z2);
      this.x2 = Math.max(x1, x2);
      this.y2 = Math.max(y1, y2);
      this.z2 = Math.max(z1, z2);
   }

   public void forEachPoint(Box.XYZFunction function) {
      for (int x = this.x1; x <= this.x2; x++) {
         for (int y = this.y1; y <= this.y2; y++) {
            for (int z = this.z1; z <= this.z2; z++) {
               function.apply(x, y, z);
            }
         }
      }
   }

   public boolean allMatch(Box.XYZPredicate predicate) {
      for (int x = this.x1; x <= this.x2; x++) {
         for (int y = this.y1; y <= this.y2; y++) {
            for (int z = this.z1; z <= this.z2; z++) {
               if (!predicate.test(x, y, z)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public Box add(Box o) {
      return new Box(this.x1 + o.x1, this.y1 + o.y1, this.z1 + o.z1, this.x2 + o.x2, this.y2 + o.y2, this.z2 + o.z2);
   }

   public Box.Mutable asMutable() {
      return new Box.Mutable(this.x1, this.y1, this.z1, this.x2, this.y2, this.z2);
   }

   public static class Mutable extends Box {
      public Mutable(int x1, int y1, int z1, int x2, int y2, int z2) {
         super(x1, y1, z1, x2, y2, z2);
      }

      public int getX1() {
         return this.x1;
      }

      public void setX1(int x1) {
         this.x1 = x1;
      }

      public int getY1() {
         return this.y1;
      }

      public void setY1(int y1) {
         this.y1 = y1;
      }

      public int getZ1() {
         return this.z1;
      }

      public void setZ1(int z1) {
         this.z1 = z1;
      }

      public int getX2() {
         return this.x2;
      }

      public void setX2(int x2) {
         this.x2 = x2;
      }

      public int getY2() {
         return this.y2;
      }

      public void setY2(int y2) {
         this.y2 = y2;
      }

      public int getZ2() {
         return this.z2;
      }

      public void setZ2(int z2) {
         this.z2 = z2;
      }

      public Box.Mutable expand(Box box) {
         this.x1 = Math.min(box.x1, this.x1);
         this.y1 = Math.min(box.y1, this.y1);
         this.z1 = Math.min(box.z1, this.z1);
         this.x2 = Math.max(box.x2, this.x2);
         this.y2 = Math.max(box.y2, this.y2);
         this.z2 = Math.max(box.z2, this.z2);
         return this;
      }

      public Box.Mutable add(int dx, int dy, int dz) {
         this.x1 += dx;
         this.x2 += dx;
         this.y1 += dy;
         this.y2 += dy;
         this.z1 += dz;
         this.z2 += dz;
         return this;
      }
   }

   @FunctionalInterface
   public interface XYZFunction {
      void apply(int var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface XYZPredicate {
      boolean test(int var1, int var2, int var3);
   }
}
