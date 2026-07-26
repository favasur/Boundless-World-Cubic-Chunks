package io.github.opencubicchunks.cubicchunks.core.asm;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class CubicChunksMixinConfig implements IMixinConfigPlugin {
   @Nonnull
   public static Logger LOGGER = LogManager.getLogger("CubicChunksMixinConfig");
   private final Object2BooleanMap<String> modDependencyConditions = new Object2BooleanLinkedOpenHashMap();

   public CubicChunksMixinConfig() {
   }

   @Override
   public void onLoad(String mixinPackage) {
      String optifineVersion = System.getProperty("cubicchunks.optifineVersion", null);
      if (optifineVersion == null) {
         try {
            Class optifineInstallerClass = Class.forName("optifine.Installer");
            Method getVersionHandler = optifineInstallerClass.getMethod("getOptiFineVersion");
            String var13 = (String)getVersionHandler.invoke(null);
            String var14 = var13.replace("_pre", "");
            optifineVersion = var14.substring(var14.length() - 2);
            LOGGER.info("Detected Optifine version: " + optifineVersion);
         } catch (ClassNotFoundException var11) {
            optifineVersion = null;
            LOGGER.info("No Optifine detected");
         } catch (Exception var12) {
            LOGGER.error("Optifine detected, but could not detect version. It may not work. Assuming Optifine E1...", var12);
            optifineVersion = "E1";
         }
      }

      CubicChunksMixinConfig.OptifineState optifineState;
      if (optifineVersion == null) {
         optifineState = CubicChunksMixinConfig.OptifineState.NOT_LOADED;
      } else if (optifineVersion.compareTo("G5") > 0) {
         LOGGER.error("Unknown optifine version: " + optifineVersion + ", it may not work. Assuming E1-G5.");
         optifineState = CubicChunksMixinConfig.OptifineState.LOADED_E1;
      } else if (optifineVersion.compareTo("E1") >= 0) {
         optifineState = CubicChunksMixinConfig.OptifineState.LOADED_E1;
      } else {
         new RuntimeException("Unsupported optifine version " + optifineVersion + ", trying E1-G5 specific mixins").printStackTrace();
         optifineState = CubicChunksMixinConfig.OptifineState.LOADED_E1;
      }

      this.modDependencyConditions.defaultReturnValue(true);
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinRenderGlobalNoOptifine",
            optifineState == CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.vertviewdist.MixinRenderGlobalNoOptifine",
            optifineState == CubicChunksMixinConfig.OptifineState.NOT_LOADED && CubicChunksMixinConfig.BoolOptions.VERT_RENDER_DISTANCE.getValue()
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderGlobalOptifine_E",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderChunk",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderChunkUtils",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinExtendedBlockStorage",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinRenderList",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinViewFrustum",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine.MixinChunkVisibility",
            optifineState != CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      this.modDependencyConditions
         .put(
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.IGuiVideoSettings",
            optifineState == CubicChunksMixinConfig.OptifineState.NOT_LOADED
         );
      boolean enableBetterFpsBeaconFix = false;

      try {
         Class betterFpsConditions = Class.forName("guichaguri.betterfps.transformers.Conditions");
         Method getFixSetting = betterFpsConditions.getMethod("shouldPatch", String.class);
         boolean betterFpsFastBeaconActive = (Boolean)getFixSetting.invoke(null, "fastBeacon");
         if (betterFpsFastBeaconActive) {
            enableBetterFpsBeaconFix = true;
            LOGGER.info("BetterFps FastBeacon active, will activate mixin for beacons with FastBeacon.");
         } else {
            LOGGER.info("BetterFps is installed, but FastBeacon is not active. Will not enable FastBeacon mixin.");
         }
      } catch (ClassNotFoundException var9) {
         LOGGER.info("BetterFps is NOT installed. Will not enable FastBeacon mixin.");
      } catch (Exception var10) {
         LOGGER.info("Problem trying to detect BetterFps settings. Will not enable FastBeacon mixin.");
      }

      this.modDependencyConditions
         .put("io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinTileEntityBeaconBetterFps", enableBetterFpsBeaconFix);
      File folder = new File(".", "config");
      folder.mkdirs();
      File configFile = new File(folder, "cubicchunks_mixin_config.json");
      LOGGER.info("Loading configuration file " + configFile.getAbsolutePath());

      try {
         if (!configFile.exists()) {
            this.writeConfigToJson(configFile);
         }

         this.readConfigFromJson(configFile);
      } catch (IOException var8) {
         var8.printStackTrace();
      }
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return this.shouldApplyMixin(mixinClassName);
   }

   public boolean shouldApplyMixin(String mixinClassName) {
      for (CubicChunksMixinConfig.BoolOptions configOption : CubicChunksMixinConfig.BoolOptions.values()) {
         for (String mixinClassNameOnTrue : configOption.mixinClassNamesOnTrue) {
            if (mixinClassName.equals(mixinClassNameOnTrue)) {
               boolean load = configOption.value && this.modDependencyConditions.getBoolean(mixinClassName);
               LOGGER.debug("shouldApplyMixin({}) = {} from {}.mixinClassNamesOnTrue", mixinClassName, load, configOption);
               return load;
            }
         }

         for (String mixinClassNameOnFalse : configOption.mixinClassNamesOnFalse) {
            if (mixinClassName.equals(mixinClassNameOnFalse)) {
               boolean load = !configOption.value && this.modDependencyConditions.getBoolean(mixinClassName);
               LOGGER.debug("shouldApplyMixin({}) = {} from {}.mixinClassNamesOnFalse", mixinClassName, load, configOption);
               return load;
            }
         }
      }

      boolean load = this.modDependencyConditions.getBoolean(mixinClassName);
      LOGGER.debug("shouldApplyMixin({}) = {} from modDependencyConditions", mixinClassName, load);
      return load;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public static String getNicelyFormattedName(String name) {
      StringBuffer out = new StringBuffer();
      char char_ = '_';
      char prevchar = 0;

      for (char c : name.toCharArray()) {
         if (c != char_ && prevchar != char_) {
            out.append(String.valueOf(c).toLowerCase());
         } else if (c != char_) {
            out.append(String.valueOf(c));
         }

         prevchar = c;
      }

      return out.toString();
   }

   private void writeConfigToJson(File configFile) throws IOException {
      JsonWriter writer = new JsonWriter(new FileWriter(configFile));
      writer.setIndent(" ");
      writer.beginArray();

      for (CubicChunksMixinConfig.BoolOptions configOption : CubicChunksMixinConfig.BoolOptions.values()) {
         writer.beginObject();
         writer.name(configOption.name());
         writer.value(configOption.value);
         writer.name("description");
         writer.value(configOption.description);
         writer.endObject();
      }

      writer.endArray();
      writer.close();
   }

   private void readConfigFromJson(File configFile) throws IOException {
      int expectingOptionsNumber = CubicChunksMixinConfig.BoolOptions.values().length;
      JsonReader reader = new JsonReader(new FileReader(configFile));
      reader.beginArray();

      while (reader.hasNext()) {
         reader.beginObject();

         label34:
         while (reader.hasNext()) {
            String name = reader.nextName();

            for (CubicChunksMixinConfig.BoolOptions option : CubicChunksMixinConfig.BoolOptions.values()) {
               if (option.name().equals(name)) {
                  expectingOptionsNumber--;
                  option.value = reader.nextBoolean();
                  continue label34;
               }
            }

            reader.skipValue();
         }

         reader.endObject();
      }

      reader.endArray();
      reader.close();
      if (expectingOptionsNumber != 0) {
         this.writeConfigToJson(configFile);
      }
   }

   public static enum BoolOptions {
      OPTIMIZE_PATH_NAVIGATOR(
         false,
         new String[0],
         new String[]{
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinPathNavigate",
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWalkNodeProcessor"
         },
         "Enabling this option will optimize work of vanilla path navigator.Using this option in some cases turn entity AI a little dumber. Mob standing in a single axis aligned line with player in a middle of a chunk will not try to seek path to player outside of chunks if direct path is blocked. You need to restart Minecraft to apply changes."
      ),
      USE_CUBE_ARRAYS_INSIDE_CHUNK_CACHE(
         true,
         new String[]{"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinChunkCache_Vanilla"},
         new String[]{
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinChunkCache",
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinChunkCache_Cubic"
         },
         "Enabling this option will mix cube array into chunk cache for using in entity path navigator. Potentially this will slightly reduce server tick time in presence of huge amount of living entities. You need to restart Minecraft to apply changes."
      ),
      USE_FAST_COLLISION_CHECK(
         false,
         new String[]{"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWorld_SlowCollisionCheck"},
         new String[]{"io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common.MixinWorld_CollisionCheck"},
         "Enabling this option allow using fast collision check. Fast collision check can reduce server lag. You need to restart Minecraft to apply changes."
      ),
      VERT_RENDER_DISTANCE(
         true,
         new String[0],
         new String[]{
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinEntityRenderer",
            "io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.MixinRenderGlobal"
         },
         "Enabling this option will make the vertical view distance slider affect clientside vertical render distance. When disabled, only serverside load distance is affected."
      );

      private final boolean defaultValue;
      private final String[] mixinClassNamesOnFalse;
      private final String[] mixinClassNamesOnTrue;
      private final String description;
      private boolean value;

      private BoolOptions(boolean defaultValue1, String[] mixinClassNamesOnFalse1, String[] mixinClassNamesOnTrue1, String description1) {
         this.defaultValue = defaultValue1;
         this.mixinClassNamesOnFalse = mixinClassNamesOnFalse1;
         this.mixinClassNamesOnTrue = mixinClassNamesOnTrue1;
         this.description = description1;
         this.value = this.defaultValue;
      }

      public boolean getValue() {
         return this.value;
      }
   }

   private static enum OptifineState {
      NOT_LOADED,
      LOADED_E1;

      private OptifineState() {
      }
   }
}
