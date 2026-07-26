package io.github.opencubicchunks.cubicchunks.core;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Ignore;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.RequiresWorldRestart;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(
   modid = "cubicchunks"
)
@Config(
   modid = "cubicchunks",
   category = "general"
)
public class CubicChunksConfig {
   @Comment({"Chunk garbage collector update interval. Lower value will increase CPU usage, but can reduce memory usage."})
   @LangKey("cubicchunks.config.chunk_gc_interval")
   public static int chunkGCInterval = 200;
   @Comment({"Eliminates a few data copies in compatibility generator. May break some mods. Disable if you experience issues in modded dimensions or world types"})
   @LangKey("cubicchunks.config.optimized_compatibility_generator")
   public static boolean optimizedCompatibilityGenerator = true;
   @LangKey("cubicchunks.config.force_cc")
   @Comment({"Determines when a cubic chunks world should be created for non-cubic-chunks world types.\nNONE - only when cubic chunks world type\nNEW_WORLD - only for newly created worlds\nLOAD_NOT_EXCLUDED - load all worlds as cubic chunks, except excluded dimensions\nALWAYS - load everything as cubic chunks. Overrides forceDimensionExcludes"})
   public static CubicChunksConfig.ForceCCMode forceLoadCubicChunks = CubicChunksConfig.ForceCCMode.NONE;
   @LangKey("cubicchunks.config.cubegen_per_tick")
   @Comment({"The maximum number of cubic chunks to generate per tick."})
   public static int maxGeneratedCubesPerTick = 784;
   @LangKey("cubicchunks.config.max_cube_generation_time_millis")
   @Comment({"Maximum amount of time spent on generating chunks per dimension."})
   public static int maxCubeGenerationTimeMillis = 50;
   @LangKey("cubicchunks.config.use_vanilla_world_generators")
   @Comment({"Enabling this option will force cubic chunks to use world generators designed for two dimensional chunks, which are often used for custom ore generators added by mods. To do so cubic chunks will pregenerate cubes in a range of height from 0 to 255. This is very likely to break a lot of mods, cause the game to hang, and make the world generation depend on the order in which things are generated. Use at your own risk."})
   public static boolean useVanillaChunkWorldGenerators = false;
   @LangKey("cubicchunks.config.vert_view_distance")
   @Comment({"Similar to Minecraft's view distance, only for vertical chunks. Automatically adjusted by vertical view distance slider on the client. Does not affect rendering, only what chunks are sent to client."})
   public static int verticalCubeLoadDistance = 8;
   @LangKey("cubicchunks.config.dimension_blacklist")
   @Comment({"The specified dimension ID ranges won't be created as cubic chunks world for new worlds, and worlds created before this option has been added, unless forceDimensionExcludes is set to true. IDs can be specified either as range in format min:max, or as single numbers.\nExample:\n    S:excludedDimensions <\n        1\n        10:100\n        101:200\n        -5\n     >\nThe ranges specified can overlap, and the bounds can be specified in reversed order."})
   public static String[] excludedDimensions = new String[]{"1"};
   @LangKey("cubicchunks.config.force_dimension_blacklist")
   @Comment({"If this is set to true, cubic chunks will respect excluded dimensions even for already existing worlds. If this results in a existing dimension switching between cubic chunks and vanilla, the contents of that dimension won't be converted."})
   public static boolean forceDimensionExcludes = false;
   @LangKey("cubicchunks.config.relight_checks_per_tick_per_column")
   @Comment({"In an attempt to fix lighting glitches over time, cubic chunks will keep updating light in specified amount of blocks per column (chunk) per tick. Default value of 1 doesn't cause noticeable performance drop, but still fixes most major issues relatively quickly."})
   public static int relightChecksPerTickPerColumn = 1;
   @LangKey("cubicchunks.config.do_client_light_fixes")
   @Comment({"By default cubic chunks will attempt to go over all the blocks over time to fix lighting only on server. Enable this to also fix lighting on the clientside."})
   public static boolean doClientLightFixes = false;
   @LangKey("cubicchunks.config.biome_temperature_center_y")
   @Comment({"Heights below this value will have normal, unmodified biome temperature"})
   public static int biomeTemperatureCenterY = 64;
   @LangKey("cubicchunks.config.biome_temperature_y_factor")
   @Comment({"How much should biome temperature increase with height (negative values decrease temperature)"})
   public static float biomeTemperatureHeightFactor = -0.0016666667F;
   @LangKey("cubicchunks.config.biome_temperature_scale_max_y")
   @Comment({"Above this height, biome temperature will no longer change"})
   public static int biomeTemperatureScaleMaxY = 256;
   @LangKey("cubicchunks.config.compatibility_generator_type")
   @Comment({"Vanilla compatibility generator type, which will convert vanilla world type generators output in cubic"})
   public static String compatibilityGeneratorType = "cubicchunks:default";
   @LangKey("cubicchunks.config.storage_format")
   @Comment({"The storage format. Note: this will be used for all newly created worlds. Existing worlds will continue to use the format they were created with.\nIf empty, the storage format for new worlds will be determined automatically."})
   public static String storageFormat = "";
   @LangKey("cubicchunks.config.spawn_generate_distance_horizontal")
   @Comment({"Horizontal distance for initially generated spawn area"})
   @RequiresWorldRestart
   public static int spawnGenerateDistanceXZ = 12;
   @LangKey("cubicchunks.config.spawn_generate_distance_vertical")
   @Comment({"Vertical distance for initially generated spawn area"})
   @RequiresWorldRestart
   public static int spawnGenerateDistanceY = 8;
   @LangKey("cubicchunks.config.spawn_forceload_distance_horizontal")
   @Comment({"Horizontal distance for spawn chunks kept loaded in memory"})
   @RequiresWorldRestart
   public static int spawnLoadDistanceXZ = 8;
   @LangKey("cubicchunks.config.spawn_forceload_distance_vertical")
   @Comment({"Vertical distance for spawn chunks kept loaded in memory"})
   @RequiresWorldRestart
   public static int spawnLoadDistanceY = 8;
   @LangKey("cubicchunks.config.default_min_height")
   @Comment({"World min height. Values that are not an integer multiple of 16 may cause unintended behavior"})
   @RangeInt(
      min = -2147479552,
      max = 0
   )
   public static int defaultMinHeight = -1073741824;
   @LangKey("cubicchunks.config.default_max_height")
   @Comment({"World max height. Values that are not an integer multiple of 16 may cause unintended behavior"})
   @RangeInt(
      min = 16,
      max = 2147479552
   )
   public static int defaultMaxHeight = 1073741824;
   @LangKey("cubicchunks.config.replace_light_recheck")
   @Comment({"Replaces vanilla light check code with cubic chunks code for cubic chunks worlds.\nCubic chunks version keeps track of light changes on the server and sends them to client\nand handles the edge of the world by scheduling chunk edge updates instead of failing."})
   public static boolean replaceLightRecheck = false;
   @LangKey("cubicchunks.config.update_known_broken_lighting_on_load")
   @Comment({"Attempts to detect worlds saved with cubic chunks versions with lighting glitches, and fix them on world load."})
   public static boolean updateKnownBrokenLightingOnLoad = true;
   @LangKey("cubicchunks.config.worldgen_watchdog_time_limit")
   @Comment({"Maximum amount of time (milliseconds) generating a single chunk can take in vanilla compatibility generator before forcing a crash."})
   public static int worldgenWatchdogTimeLimit = 10000;
   @LangKey("cubicchunks.config.allow_vanilla_clients")
   @Comment({"Allows clients without cubic chunks to join. THIS IS INTENDED FOR VANILLA CLIENTS. This is VERY likely to break when used with other mods"})
   public static boolean allowVanillaClients = false;
   @LangKey("cubicchunks.config.fast_simplified_sky_light")
   @Comment({"Forces an MC-classic-like skylight propagation algorithm. It's much faster and doesn't look too bad. You can enable it if you don't need normal skylight values but want extra performance for worldgen and block updates"})
   public static boolean fastSimplifiedSkyLight = false;
   @LangKey("cubicchunks.config.cubes_to_send_per_tick")
   @Comment({"Max amount of cubes sent to client per tick to players"})
   public static int cubesToSendPerTick = 649;
   @LangKey("cubicchunks.config.vanilla_clients")
   @Comment({"Options relating to support for vanilla clients."})
   public static CubicChunksConfig.VanillaClients vanillaClients = new CubicChunksConfig.VanillaClients();
   @LangKey("cubicchunks.config.use_shadow_paging_io")
   @Comment({"Whether cubic chunks save format IO should use shadow paging. This may be slightly slower and use a bit more storage but should significantly improve reliability in case of improper shutdown."})
   @RequiresWorldRestart
   public static boolean useShadowPagingIO = true;
   public static int defaultMaxCubesPerChunkloadingTicket = 400;
   public static Map<String, Integer> modMaxCubesPerChunkloadingTicket = new HashMap<>();
   @Ignore
   private static TreeRangeSet<Integer> excludedDimensionsRanges = null;

   public CubicChunksConfig() {
   }

   public static void sync() {
      ConfigManager.sync("cubicchunks", Type.INSTANCE);
      initDimensionRanges();
   }

   private static void initDimensionRanges() {
      if (excludedDimensionsRanges == null) {
         excludedDimensionsRanges = TreeRangeSet.create();
      }

      excludedDimensionsRanges.clear();
      Predicate<String> NUMBER_PATTERN = Pattern.compile("^-?\\d+$").asPredicate();
      Predicate<String> NUMBER_RANGE_PATTERN = Pattern.compile("^-?\\d+:-?\\d+$").asPredicate();
      int i = 0;

      for (String str : excludedDimensions) {
         if (NUMBER_PATTERN.test(str)) {
            excludedDimensionsRanges.add(Range.singleton(Integer.parseInt(str)));
         } else {
            if (!NUMBER_RANGE_PATTERN.test(str)) {
               throw new NumberFormatException(str);
            }

            String[] parts = str.split(":");
            excludedDimensionsRanges.add(Range.closed(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
         }

         i++;
      }
   }

   @SubscribeEvent
   public static void onConfigChanged(OnConfigChangedEvent event) {
      if (event.getModID().equals("cubicchunks")) {
         sync();
      }
   }

   public static void setVerticalViewDistance(int value) {
      verticalCubeLoadDistance = value;
      sync();
   }

   public static void disableCubicChunks() {
      forceLoadCubicChunks = CubicChunksConfig.ForceCCMode.NONE;
      sync();
   }

   public static void setGenerator(ResourceLocation generatorTypeIn) {
      if (forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.NONE) {
         forceLoadCubicChunks = CubicChunksConfig.ForceCCMode.NEW_WORLD;
      }

      compatibilityGeneratorType = generatorTypeIn.toString();
      sync();
   }

   public static boolean isDimensionExcluded(int dimension) {
      if (excludedDimensionsRanges == null) {
         initDimensionRanges();
      }

      return excludedDimensionsRanges.contains(dimension);
   }

   static {
      modMaxCubesPerChunkloadingTicket.put("cubicchunks", defaultMaxCubesPerChunkloadingTicket);
   }

   public static enum ForceCCMode {
      NONE,
      NEW_WORLD,
      LOAD_NOT_EXCLUDED,
      ALWAYS;

      private ForceCCMode() {
      }
   }

   public static final class VanillaClients {
      @LangKey("cubicchunks.config.vanilla_clients.horizontal_slices")
      @Comment({"Enables horizontal slices for vanilla clients. This will cause coordinates to wrap around on the X and Z axes in the same way as on Y."})
      public boolean horizontalSlices = true;
      @LangKey("cubicchunks.config.vanilla_clients.horizontal_slices_bedrock_only")
      @Comment({"If horizontal slices is enabled, restricts horizontal slices to Bedrock edition clients.\nNote that Bedrock clients are not supported directly, but only when connecting through a proxy such as Geyser."})
      public boolean horizontalSlicesBedrockOnly = true;
      @LangKey("cubicchunks.config.vanilla_clients.horizontal_slice_size")
      @Comment({"The size (radius) of a horizontal slice."})
      public int horizontalSliceSize = 65536;

      public VanillaClients() {
      }
   }
}
