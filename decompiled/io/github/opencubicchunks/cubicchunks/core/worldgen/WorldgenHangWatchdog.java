package io.github.opencubicchunks.cubicchunks.core.worldgen;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class WorldgenHangWatchdog {
   public static final boolean ENABLED = "true".equalsIgnoreCase(System.getProperty("cubicchunks.wgen_hang_watchdog", "true"));
   private static final WorldgenHangWatchdog INSTANCE = new WorldgenHangWatchdog();
   private static final Thread thread = init();
   private final WeakHashMap<Thread, WorldgenHangWatchdog.Entry> entries = new WeakHashMap<>();
   private static volatile String crashInfo = null;

   private WorldgenHangWatchdog() {
      if (INSTANCE != null) {
         throw new IllegalStateException("Already initialized");
      }
   }

   public static String getCrashInfo() {
      return crashInfo;
   }

   public static void startWorldGen() {
      synchronized (INSTANCE.entries) {
         INSTANCE.entries.compute(Thread.currentThread(), (t, old) -> {
            if (old == null) {
               return new WorldgenHangWatchdog.Entry();
            } else {
               old.count++;
               return (WorldgenHangWatchdog.Entry)old;
            }
         });
      }
   }

   public static void endWorldGen() {
      synchronized (INSTANCE.entries) {
         WorldgenHangWatchdog.Entry e = INSTANCE.entries.get(Thread.currentThread());
         if (e != null) {
            if (e.count <= 0) {
               INSTANCE.entries.remove(Thread.currentThread());
            } else {
               e.count--;
            }
         }
      }
   }

   private static Thread init() {
      Thread t = new Thread(INSTANCE::run);
      t.setName("WorldGen hang watchdog thread");
      t.setDaemon(true);
      t.start();
      return t;
   }

   private void run() {
      if (ENABLED) {
         while (true) {
            try {
               Thread.sleep(500L);
            } catch (InterruptedException var20) {
               var20.printStackTrace();
            }

            synchronized (this.entries) {
               Iterator<Map.Entry<Thread, WorldgenHangWatchdog.Entry>> iterator = this.entries.entrySet().iterator();

               while (iterator.hasNext()) {
                  Map.Entry<Thread, WorldgenHangWatchdog.Entry> entry = iterator.next();
                  Thread t = entry.getKey();
                  WorldgenHangWatchdog.Entry e = entry.getValue();
                  e.samples.add(t.getStackTrace());
                  long currentTime = System.nanoTime();
                  long dt = currentTime - e.startTime;
                  if (dt > TimeUnit.MILLISECONDS.toNanos((long)CubicChunksConfig.worldgenWatchdogTimeLimit)) {
                     StringBuilder sb = new StringBuilder();
                     sb.append("World generation taking ")
                        .append((double)dt / (double)TimeUnit.SECONDS.toNanos(1L))
                        .append(" seconds, should be less than 50ms. Stopping the server.\n");
                     sb.append("Samples collected during world generation:\n");
                     int i = 1;

                     for (StackTraceElement[] stacktrace : e.samples) {
                        sb.append("--------------------------------------------\n");
                        Set<String> likelyModsInvolved = CompatHandler.getModsForStacktrace(stacktrace);
                        sb.append("SAMPLE #").append(i).append(", likely mods involved: ").append(String.join(", ", likelyModsInvolved)).append('\n');

                        for (StackTraceElement traceElement : stacktrace) {
                           sb.append("\tat ").append(traceElement).append('\n');
                        }

                        i++;
                     }

                     String msg = sb.toString();
                     crashInfo = msg;
                     CubicChunks.LOGGER.fatal(msg);
                     t.stop();
                     iterator.remove();
                  }
               }
            }
         }
      }
   }

   private static class Entry {
      long startTime;
      int count;
      List<StackTraceElement[]> samples = new ArrayList<>();

      Entry() {
         this.startTime = System.nanoTime();
      }
   }
}
