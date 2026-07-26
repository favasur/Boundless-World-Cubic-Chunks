package io.github.opencubicchunks.cubicchunks.api.util;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.MathHelper;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MathUtil {
   public MathUtil() {
   }

   public static boolean isPowerOfN(int toTest, int n) {
      while (toTest > n - 1 && toTest % n == 0) {
         toTest /= n;
      }

      return toTest == 1;
   }

   public static double lerp(double a, double min, double max) {
      return min + a * (max - min);
   }

   public static double unlerp(double v, double min, double max) {
      return (v - min) / (max - min);
   }

   public static float unlerp(float v, float min, float max) {
      return (v - min) / (max - min);
   }

   public static float unlerp(long v, long min, long max) {
      return (float)(v - min) / (float)(max - min);
   }

   public static float lerp(float a, float min, float max) {
      return min + a * (max - min);
   }

   public static int min(int a, int b) {
      return Math.min(a, b);
   }

   public static int min(int a, int b, int c) {
      return Math.min(Math.min(a, b), c);
   }

   public static int min(int a, int b, int c, int d) {
      return Math.min(Math.min(a, b), Math.min(c, d));
   }

   public static int min(int... a) {
      int min = a[0];

      for (int i = 1; i < a.length; i++) {
         if (a[i] < min) {
            min = a[i];
         }
      }

      return min;
   }

   public static int max(int a, int b) {
      return Math.max(a, b);
   }

   public static int max(int a, int b, int c) {
      return Math.max(Math.max(a, b), c);
   }

   public static int max(int a, int b, int c, int d) {
      return Math.max(Math.max(a, b), Math.max(c, d));
   }

   public static int max(int... a) {
      int max = a[0];

      for (int i = 1; i < a.length; i++) {
         if (a[i] > max) {
            max = a[i];
         }
      }

      return max;
   }

   public static float maxIgnoreNan(float... a) {
      float max = a[0];

      for (int i = 1; i < a.length; i++) {
         if (a[i] > max || Float.isNaN(max)) {
            max = a[i];
         }
      }

      if (Float.isNaN(max)) {
         throw new IllegalArgumentException("All values are NaN");
      } else {
         return max;
      }
   }

   public static double gaussianProbabilityDensity(double x, double mean, double stdDev) {
      return Math.exp(-(x - mean) * (x - mean) / (2.0 * stdDev * stdDev)) / (Math.sqrt(Math.PI * 2) * stdDev);
   }

   public static double bellCurveProbabilityCyclic(int x, int mean, double stdDev, int spacing) {
      double halfSpace = (double)spacing / 2.0;
      double shiftedLoc = (double)x - halfSpace - (double)mean;
      double factor = Math.abs(shiftedLoc % (double)spacing) - halfSpace;
      double divisorExp = 2.0 * stdDev * stdDev;
      double exponent = -1.0 * factor * factor / divisorExp;
      return Math.exp(exponent);
   }

   public static boolean rangesIntersect(int min1, int max1, int min2, int max2) {
      return min1 <= max2 && min2 <= max1;
   }

   public static int packColorARGB(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int to8bitComponent(float value) {
      return MathHelper.func_76125_a(Math.round(value * 255.0F), 0, 255);
   }
}
