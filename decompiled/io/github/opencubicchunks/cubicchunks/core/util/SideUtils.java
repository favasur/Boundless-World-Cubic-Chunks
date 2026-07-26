package io.github.opencubicchunks.cubicchunks.core.util;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class SideUtils {
   public SideUtils() {
   }

   public static <T> T getForSide(Supplier<Supplier<T>> client, Supplier<Supplier<T>> server) {
      return FMLCommonHandler.instance().getSide().isClient() ? client.get().get() : server.get().get();
   }

   public static <T, R> R getForSide(T param, Supplier<Function<T, R>> client, Supplier<Function<T, R>> server) {
      return FMLCommonHandler.instance().getSide().isClient() ? client.get().apply(param) : server.get().apply(param);
   }

   public static void runForSide(Supplier<Runnable> client, Supplier<Runnable> server) {
      if (FMLCommonHandler.instance().getSide().isClient()) {
         client.get().run();
      } else {
         server.get().run();
      }
   }

   public static void runForClient(Supplier<Runnable> toRun) {
      if (FMLCommonHandler.instance().getSide().isClient()) {
         toRun.get().run();
      }
   }
}
