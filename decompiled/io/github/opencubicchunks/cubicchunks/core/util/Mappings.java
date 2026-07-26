package io.github.opencubicchunks.cubicchunks.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Mappings {
   private static boolean IS_DEV;
   private static final Map<String, String> srgToMcp = new HashMap<>();

   public Mappings() {
   }

   public static String getNameFromSrg(String srgName) {
      if (IS_DEV) {
         String result = srgToMcp.get(srgName);
         return result == null ? srgName : result;
      } else {
         return srgName;
      }
   }

   private static void initMappings(String property) {
      try (Scanner scanner = new Scanner(new File(property))) {
         while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            parseLine(line);
         }
      } catch (FileNotFoundException var14) {
         throw new RuntimeException(var14);
      }
   }

   private static void parseLine(String line) {
      if (line.startsWith("FD: ")) {
         parseField(line.substring("FD: ".length()));
      }

      if (line.startsWith("MD: ")) {
         parseMethod(line.substring("MD: ".length()));
      }
   }

   private static void parseMethod(String substring) {
      String[] s = substring.split(" ");
      int SRG_NAME = 0;
      int MCP_NAME = 2;
      int lastIndex = s[0].lastIndexOf(47) + 1;
      if (lastIndex < 0) {
         lastIndex = 0;
      }

      s[0] = s[0].substring(lastIndex);
      lastIndex = s[2].lastIndexOf("/") + 1;
      if (lastIndex < 0) {
         lastIndex = 0;
      }

      s[2] = s[2].substring(lastIndex);
      srgToMcp.put(s[0], s[2]);
   }

   private static void parseField(String str) {
      if (str.contains(" ")) {
         String[] s = str.split(" ");

         assert s.length == 2;

         int lastIndex = s[0].lastIndexOf(47) + 1;
         if (lastIndex < 0) {
            lastIndex = 0;
         }

         s[0] = s[0].substring(lastIndex);
         lastIndex = s[1].lastIndexOf("/") + 1;
         if (lastIndex < 0) {
            lastIndex = 0;
         }

         s[1] = s[1].substring(lastIndex);
         srgToMcp.put(s[0], s[1]);
      }
   }

   static {
      String location = System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp");
      IS_DEV = location != null;
      if (IS_DEV) {
         initMappings(location);
      }
   }
}
