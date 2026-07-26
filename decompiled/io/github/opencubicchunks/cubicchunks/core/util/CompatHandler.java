package io.github.opencubicchunks.cubicchunks.core.util;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight.IASMEventHandler;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight.IEventBus;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Post;
import net.minecraftforge.event.terraingen.PopulateChunkEvent.Pre;
import net.minecraftforge.event.world.ChunkEvent.Load;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;
import sun.misc.Unsafe;

public class CompatHandler {
   private static final Set<String> IWORLDGENERATOR_FAKE_HEIGHT = ImmutableSet.of("ic2", "thaumcraft", "fossil", "realistictorches", "iceandfire");
   private static final Set<String> POPULATE_EVENT_PRE_FAKE_HEIGHT = ImmutableSet.of("reccomplex");
   private static final Set<String> DECORATE_EVENT_FAKE_HEIGHT = ImmutableSet.of("reccomplex");
   private static final Set<String> POST_DECORATE_EVENT_FAKE_HEIGHT = ImmutableSet.of("joshxmas");
   private static final Set<String> FAKE_CHUNK_LOAD = ImmutableSet.of("zerocore");
   private static Set<String> vanillaCompatPopulationFakeHeight = ImmutableSet.of("biomesoplenty");
   private static final Map<String, String> packageToModId = getPackageToModId();
   private static IEventListener[] fakeChunkLoadListeners;

   public CompatHandler() {
   }

   public static void init() {
      Chunk uninitializedChunk;
      try {
         Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
         theUnsafe.setAccessible(true);
         Unsafe unsafe = (Unsafe)theUnsafe.get(null);
         uninitializedChunk = (Chunk)unsafe.allocateInstance(Chunk.class);
      } catch (ReflectiveOperationException var3) {
         throw new RuntimeException(var3);
      }

      fakeChunkLoadListeners = getFakeEventListeners(new Load(uninitializedChunk).getListenerList(), MinecraftForge.EVENT_BUS, FAKE_CHUNK_LOAD);
   }

   private static Map<String, String> getPackageToModId() {
      return Collections.unmodifiableMap(
         Loader.instance()
            .getActiveModList()
            .stream()
            .flatMap(mod -> mod.getOwnedPackages().stream().map(pkg -> new SimpleEntry<>(pkg, mod.getModId())))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue, (a, b) -> a.equals(b) ? a : a + " or " + b))
      );
   }

   private static String getPackageName(Class<?> clazz) {
      String canonicalName = clazz.getCanonicalName();
      int dot = canonicalName.lastIndexOf(46);
      return dot < 0 ? "" : canonicalName.substring(0, dot);
   }

   public static Set<String> getModsForStacktrace(StackTraceElement[] stacktrace) {
      Set<String> mods = new HashSet<>();

      for (StackTraceElement traceElement : stacktrace) {
         try {
            Class<?> cl = Class.forName(traceElement.getClassName());
            if (cl != null) {
               String modid = packageToModId.get(getPackageName(cl));
               if (modid != null && !modid.equals("minecraft") && !modid.equals("forge") && !modid.equals("cubicchunks")) {
                  mods.add(modid);
               }
            }
         } catch (ClassNotFoundException var8) {
         }
      }

      return mods;
   }

   public static void beforePopulate(World world, IChunkGenerator vanilla) {
      String modid = packageToModId.get(getPackageName(vanilla.getClass()));
      if (vanillaCompatPopulationFakeHeight.contains(modid)) {
         ((ICubicWorldInternal.Server)world).fakeWorldHeight(256);
      }
   }

   public static void afterPopulate(World world) {
      ((ICubicWorldInternal.Server)world).fakeWorldHeight(0);
   }

   public static void beforeGenerate(World world, IWorldGenerator generator) {
      Class<? extends IWorldGenerator> genClass = (Class<? extends IWorldGenerator>)generator.getClass();
      String modid = packageToModId.get(getPackageName(genClass));
      if (modid == null) {
         CubicChunks.bigWarning("Found IWorldGenerator %s that doesn't come from any mod! This is most likely a bug.", genClass);
      } else {
         if (IWORLDGENERATOR_FAKE_HEIGHT.contains(modid)) {
            ((ICubicWorldInternal.Server)world).fakeWorldHeight(256);
         }
      }
   }

   public static void afterGenerate(World world) {
      ((ICubicWorldInternal.Server)world).fakeWorldHeight(0);
   }

   public static boolean postChunkPopulatePreWithFakeWorldHeight(Pre event) {
      if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
         MinecraftForge.EVENT_BUS.post(event);
      }

      return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, POPULATE_EVENT_PRE_FAKE_HEIGHT);
   }

   public static boolean postBiomeDecorateWithFakeWorldHeight(Decorate event) {
      if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
         MinecraftForge.EVENT_BUS.post(event);
      }

      return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, DECORATE_EVENT_FAKE_HEIGHT);
   }

   public static boolean postBiomePostDecorateWithFakeWorldHeight(Post event) {
      if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
         MinecraftForge.EVENT_BUS.post(event);
      }

      return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, POST_DECORATE_EVENT_FAKE_HEIGHT);
   }

   private static boolean postEventPerModFakeHeight(World world, Event event, EventBus eventBus, Set<String> modIds) {
      return !((ICubicWorld)world).isCubicWorld()
         ? eventBus.post(event)
         : postEvent((ICubicWorldInternal.Server)world, event, eventBus, modIds, w -> w.fakeWorldHeight(256), w -> w.fakeWorldHeight(0));
   }

   public static void onCubeLoad(Load load) {
      if (fakeChunkLoadListeners != null && fakeChunkLoadListeners.length != 0) {
         onChunkLoadImpl(load);
      }
   }

   private static void onChunkLoadImpl(Load load) {
      IEventBus bus = (IEventBus)MinecraftForge.EVENT_BUS;
      if (!bus.isShutdown()) {
         int i = -1;

         try {
            for (i = 0; i < fakeChunkLoadListeners.length; i++) {
               IEventListener fakeChunkLoadListener = fakeChunkLoadListeners[i];
               fakeChunkLoadListener.invoke(load);
            }
         } catch (Throwable var4) {
            bus.getExceptionHandler().handleException(MinecraftForge.EVENT_BUS, load, fakeChunkLoadListeners, i, var4);
            Throwables.throwIfUnchecked(var4);
            throw new RuntimeException(var4);
         }
      }
   }

   private static <T> boolean postEvent(T ctx, Event event, EventBus eventBus, Set<String> modIds, Consumer<T> preEvt, Consumer<T> postEvt) {
      IEventBus forgeEventBus = (IEventBus)eventBus;
      if (forgeEventBus.isShutdown()) {
         return false;
      } else {
         IEventListener[] listeners = event.getListenerList().getListeners(forgeEventBus.getBusID());
         int index = 0;

         try {
            for (; index < listeners.length; index++) {
               try {
                  IEventListener listener = listeners[index];
                  if (listener instanceof IASMEventHandler) {
                     IASMEventHandler handler = (IASMEventHandler)listener;
                     String modid = handler.getOwner().getModId();
                     if (modIds.contains(modid)) {
                        preEvt.accept(ctx);
                     }
                  }

                  listener.invoke(event);
               } finally {
                  postEvt.accept(ctx);
               }
            }
         } catch (Throwable var16) {
            forgeEventBus.getExceptionHandler().handleException(eventBus, event, listeners, index, var16);
            Throwables.throwIfUnchecked(var16);
            throw new RuntimeException(var16);
         }

         return event.isCancelable() && event.isCanceled();
      }
   }

   private static <T> IEventListener[] getFakeEventListeners(ListenerList listenerList, EventBus eventBus, Set<String> modIds) {
      if (!(eventBus instanceof IEventBus)) {
         CubicChunks.LOGGER.error("Failed to initialize CompatHandler! No event bus mixin!");
         return null;
      } else {
         IEventBus forgeEventBus = (IEventBus)eventBus;
         IEventListener[] listeners = listenerList.getListeners(forgeEventBus.getBusID());
         List<IEventListener> newList = new ArrayList<>();

         for (int index = 0; index < listeners.length; index++) {
            IEventListener listener = listeners[index];
            if (listener instanceof IASMEventHandler) {
               IASMEventHandler handler = (IASMEventHandler)listener;
               String modid = handler.getOwner().getModId();
               if (modid.equals("forge")) {
                  String desc = handler.toString();
                  if (desc.startsWith("ASM: ") && desc.contains("@")) {
                     String modClass = desc.split("@")[0].substring("ASM: ".length());

                     try {
                        Class<?> cl = Class.forName(modClass);
                        String newModid = cl.getPackage() == null ? null : getPackageToModId().get(cl.getPackage().getName());
                        if (newModid != null) {
                           modid = newModid;
                        }
                     } catch (ClassNotFoundException var14) {
                     }
                  }
               }

               if (modIds.contains(modid)) {
                  newList.add(listener);
               }
            }
         }

         return newList.toArray(new IEventListener[0]);
      }
   }
}
