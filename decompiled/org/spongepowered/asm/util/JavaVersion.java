package org.spongepowered.asm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class JavaVersion {
   private static double current = 0.0;

   private JavaVersion() {
   }

   public static double current() {
      if (current == 0.0) {
         current = resolveCurrentVersion();
      }

      return current;
   }

   private static double resolveCurrentVersion() {
      String version = System.getProperty("java.version");
      Matcher decimalMatcher = Pattern.compile("[0-9]+\\.[0-9]+").matcher(version);
      if (decimalMatcher.find()) {
         return Double.parseDouble(decimalMatcher.group());
      } else {
         Matcher numberMatcher = Pattern.compile("[0-9]+").matcher(version);
         return numberMatcher.find() ? Double.parseDouble(numberMatcher.group()) : 1.6;
      }
   }
}
