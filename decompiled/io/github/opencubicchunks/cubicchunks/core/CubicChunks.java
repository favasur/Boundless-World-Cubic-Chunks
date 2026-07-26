package io.github.opencubicchunks.cubicchunks.core;

import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.api.world.storage.StorageFormatProviderBase;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IIntegratedServer;
import io.github.opencubicchunks.cubicchunks.core.client.ClientEventHandler;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.RegionCubeStorage;
import io.github.opencubicchunks.cubicchunks.core.util.CompatHandler;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.util.SideUtils;
import io.github.opencubicchunks.cubicchunks.core.world.type.VanillaCubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.worldgen.WorldgenHangWatchdog;
import io.github.opencubicchunks.cubicchunks.core.worldgen.generator.vanilla.VanillaCompatibilityGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.NewRegistry;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ICrashCallable;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.NetworkModHolder;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(
   modid = "cubicchunks",
   name = "CubicChunks",
   version = "1.12.2-0.0.1208.0-SNAPSHOT",
   dependencies = "after:forge@[14.23.3.2691,]"
)
@EventBusSubscriber
public class CubicChunks {
   public static final VersionRange SUPPORTED_SERVER_VERSIONS;
   public static final VersionRange SUPPORTED_CLIENT_VERSIONS;
   public static final int MIN_SUPPORTED_BLOCK_Y = -2147479552;
   public static final int MAX_SUPPORTED_BLOCK_Y = 2147479552;
   public static final boolean DEBUG_ENABLED;
   public static final String MODID = "cubicchunks";
   public static final String VERSION = "1.12.2-0.0.1208.0-SNAPSHOT";
   @Nonnull
   public static Logger LOGGER;

   public CubicChunks() {
   }

   @EventHandler
   public void preInit(FMLPreInitializationEvent e) {
      LOGGER = e.getModLog();
      FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
         public String getLabel() {
            return "CubicChunks WorldGen Hang Watchdog samples";
         }

         public String call() throws Exception {
            String message = WorldgenHangWatchdog.getCrashInfo();
            return message == null ? "(no data)" : message;
         }
      });
      VanillaCubicWorldType.create();
      LOGGER.debug("Registered world types");
      NetworkModHolder holder = (NetworkModHolder)NetworkRegistry.INSTANCE.registry().get(Loader.instance().activeModContainer());
      holder.testVanillaAcceptance();
   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
      SideUtils.runForClient(() -> () -> MinecraftForge.EVENT_BUS.register(new ClientEventHandler()));
      PacketDispatcher.registerPackets();
   }

   @EventHandler
   public void postInit(FMLPostInitializationEvent event) {
      CompatHandler.init();
   }

   @EventHandler
   public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
      SideUtils.runForSide(() -> () -> {
            IIntegratedServer integratedServer = ReflectionUtil.cast(event.getServer());
            ICubicWorldSettings settings = ReflectionUtil.cast(integratedServer.getWorldSettings());
            if (settings.isCubic()) {
               event.getServer().func_71191_d(2147479552);
            }
         }, () -> () -> {
         });
   }

   @SubscribeEvent
   public static void registerRegistries(NewRegistry evt) {
      VanillaCompatibilityGeneratorProviderBase.init();
      StorageFormatProviderBase.init();
   }

   @SubscribeEvent
   public static void registerVanillaCompatibilityGeneratorProvider(Register<VanillaCompatibilityGeneratorProviderBase> event) {
      event.getRegistry().register((new VanillaCompatibilityGeneratorProviderBase() {
         public VanillaCompatibilityGenerator provideGenerator(IChunkGenerator vanillaChunkGenerator, World world) {
            return new VanillaCompatibilityGenerator(vanillaChunkGenerator, world);
         }
      }).setRegistryName(VanillaCompatibilityGeneratorProviderBase.DEFAULT).setUnlocalizedName("cubicchunks.gui.worldmenu.cc_default"));
   }

   @SubscribeEvent
   public static void registerAnvil3dStorageFormatProvider(Register<StorageFormatProviderBase> event) {
      event.getRegistry().register((new StorageFormatProviderBase() {
         @Override
         public ICubicStorage provideStorage(World world, Path path) throws IOException {
            return new RegionCubeStorage(path);
         }
      }).setRegistryName(StorageFormatProviderBase.DEFAULT).setUnlocalizedName("cubicchunks.gui.storagefmt.anvil3d"));
   }

   @NetworkCheckHandler
   public static boolean checkCanConnectWithMods(Map<String, String> modVersions, Side remoteSide) {
      String remoteFullVersion = modVersions.get("cubicchunks");
      if (remoteFullVersion == null) {
         return remoteSide.isClient() ? CubicChunksConfig.allowVanillaClients : true;
      } else if (!checkVersionFormat("1.12.2-0.0.1208.0-SNAPSHOT", remoteSide.isClient() ? Side.SERVER : Side.CLIENT)) {
         return true;
      } else if (!checkVersionFormat(remoteFullVersion, remoteSide)) {
         return true;
      } else {
         ArtifactVersion version = new DefaultArtifactVersion(remoteFullVersion);
         ArtifactVersion currentVersion = new DefaultArtifactVersion("1.12.2-0.0.1208.0-SNAPSHOT");
         return currentVersion.compareTo(version) < 0
            ? true
            : (remoteSide.isClient() ? SUPPORTED_CLIENT_VERSIONS : SUPPORTED_SERVER_VERSIONS).containsVersion(version);
      }
   }

   private static boolean checkVersionFormat(String version, @Nullable Side remoteSide) {
      int mcVersionSplit = version.indexOf(45);
      if (mcVersionSplit < 0) {
         LOGGER.warn(
            "Connection attempt with unexpected "
               + remoteSide
               + " version string: "
               + version
               + ". Cannot split into MC version and mod version. Assuming dev environment or special/unknown version, connection will be allowed."
         );
         return false;
      } else {
         String modVersion = version.substring(mcVersionSplit + 1);
         if (modVersion.isEmpty()) {
            LOGGER.warn(
               "Connection attempt with unexpected "
                  + remoteSide
                  + " version string: "
                  + version
                  + ". Mod version part not found. Assuming dev environment or special/unknown version,, connection will be allowed"
            );
            return false;
         } else {
            String versionRegex = "\\d+\\.\\d+\\.\\d+\\.\\d+(-.+)?";
            if (!modVersion.matches("\\d+\\.\\d+\\.\\d+\\.\\d+(-.+)?")) {
               LOGGER.warn(
                  "Connection attempt with unexpected "
                     + remoteSide
                     + " version string: "
                     + version
                     + ". Mod version part ("
                     + modVersion
                     + ") does not match expected format ('MAJORMOD.MAJORAPI.MINOR.PATCH(-optionalText)'). Assuming dev environment or special/unknown version, connection will be allowed"
               );
               return false;
            } else {
               return true;
            }
         }
      }
   }

   public static void bigWarning(String format, Object... data) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      LOGGER.log(Level.WARN, "****************************************");
      LOGGER.log(Level.WARN, "* " + format, data);

      for (int i = 2; i < 10 && i < trace.length; i++) {
         LOGGER.log(Level.WARN, "*  at {}{}", trace[i].toString(), i == 9 ? "..." : "");
      }

      LOGGER.log(Level.WARN, "****************************************");
   }

   public static boolean hasOptifine() {
      return SideUtils.getForSide(() -> () -> FMLClientHandler.instance().hasOptifine(), () -> () -> false);
   }

   static {
      try {
         SUPPORTED_SERVER_VERSIONS = VersionRange.createFromVersionSpec("[1.12.2-0.0.887.0,)");
         SUPPORTED_CLIENT_VERSIONS = VersionRange.createFromVersionSpec("[1.12.2-0.0.887.0,)");
      } catch (InvalidVersionSpecificationException var1) {
         throw new Error(var1);
      }

      DEBUG_ENABLED = System.getProperty("cubicchunks.debug", "false").equalsIgnoreCase("true");
      LOGGER = LogManager.getLogger("EarlyCubicChunks");
   }
}
