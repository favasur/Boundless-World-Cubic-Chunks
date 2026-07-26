package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import org.objectweb.asm.Opcodes;

public final class ASM {
   private static int majorVersion = 5;
   private static int minorVersion = 0;
   private static String maxVersion = "FALLBACK";
   public static final int API_VERSION = detectVersion();

   private ASM() {
   }

   public static int getApiVersionMajor() {
      return majorVersion;
   }

   public static int getApiVersionMinor() {
      return minorVersion;
   }

   public static String getApiVersionString() {
      return String.format("ASM %d.%d (%s)", majorVersion, minorVersion, maxVersion);
   }

   private static int detectVersion() {
      int apiVersion = 262144;

      for (Field field : Opcodes.class.getDeclaredFields()) {
         if (field.getType() == int.class && field.getName().startsWith("ASM")) {
            try {
               int version = field.getInt(null);
               int minor = version >> 8 & 0xFF;
               int major = version >> 16 & 0xFF;
               boolean experimental = (version >> 24 & 0xFF) != 0;
               if (major >= majorVersion) {
                  maxVersion = field.getName();
                  if (!experimental) {
                     apiVersion = version;
                     majorVersion = major;
                     minorVersion = minor;
                  }
               }
            } catch (ReflectiveOperationException var9) {
               throw new Error(var9);
            }
         }
      }

      return apiVersion;
   }
}
