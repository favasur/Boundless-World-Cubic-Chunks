package io.github.opencubicchunks.cubicchunks.api.util;

public class IntRange {
   private final int min;
   private final int max;

   public IntRange(int min, int max) {
      this.min = min;
      this.max = max;
   }

   public static IntRange single(int i) {
      return new IntRange(i, i);
   }

   public static IntRange of(int a, int b) {
      return new IntRange(Math.min(a, b), Math.max(a, b));
   }

   public int getMin() {
      return this.min;
   }

   public int getMax() {
      return this.max;
   }

   @Override
   public String toString() {
      return "IntRange{min=" + this.min + ", max=" + this.max + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         IntRange intRange = (IntRange)o;
         return this.min != intRange.min ? false : this.max == intRange.max;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.min;
      return 31 * result + this.max;
   }
}
