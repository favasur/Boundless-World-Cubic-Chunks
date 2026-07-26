package io.github.opencubicchunks.cubicchunks.core.debug;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Dbg {
   private static PrintWriter pw;

   public Dbg() {
   }

   public static void restart() {
      if (pw != null) {
         pw.close();
      }

      PrintWriter p;
      try {
         p = new PrintWriter(new File("DEBUG_" + System.currentTimeMillis()));
      } catch (FileNotFoundException var2) {
         throw new RuntimeException(var2);
      }

      pw = p;
      Runtime.getRuntime().addShutdownHook(new Thread(pw::close));
   }

   public static void p(String format, Object... objs) {
      if (CubicChunks.DEBUG_ENABLED) {
         pw.printf(format, objs);
      }
   }

   public static void l(String format, Object... objs) {
      if (CubicChunks.DEBUG_ENABLED) {
         p("[%s] ", Thread.currentThread().getName());
         p(format, objs);
         pw.println();
      }
   }

   static {
      if (!CubicChunks.DEBUG_ENABLED) {
         pw = null;
      } else {
         restart();
      }
   }
}
