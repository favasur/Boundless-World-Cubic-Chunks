package org.spongepowered.asm.mixin.transformer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.VersionNumber;

final class MixinConfig implements Comparable<MixinConfig>, IMixinConfig {
   private static int configOrder = 0;
   private static final Set<String> globalMixinList = new HashSet<>();
   private final Logger logger = LogManager.getLogger("mixin");
   private final transient Map<String, List<MixinInfo>> mixinMapping = new HashMap<>();
   private final transient Set<String> unhandledTargets = new HashSet<>();
   private final transient List<MixinInfo> pendingMixins = new ArrayList<>();
   private final transient List<MixinInfo> mixins = new ArrayList<>();
   private transient Config handle;
   private transient MixinConfig parent;
   @SerializedName("parent")
   private String parentName;
   @SerializedName("target")
   private String selector;
   @SerializedName("minVersion")
   private String version;
   @SerializedName("compatibilityLevel")
   private String compatibility;
   @SerializedName("required")
   private Boolean requiredValue;
   private transient boolean required;
   @SerializedName("priority")
   private int priority = -1;
   @SerializedName("mixinPriority")
   private int mixinPriority = -1;
   @SerializedName("package")
   private String mixinPackage;
   @SerializedName("mixins")
   private List<String> mixinClasses;
   @SerializedName("client")
   private List<String> mixinClassesClient;
   @SerializedName("server")
   private List<String> mixinClassesServer;
   @SerializedName("setSourceFile")
   private boolean setSourceFile = false;
   @SerializedName("refmap")
   private String refMapperConfig;
   @SerializedName("verbose")
   private boolean verboseLogging;
   private final transient int order = configOrder++;
   private final transient List<MixinConfig.IListener> listeners = new ArrayList<>();
   private transient IMixinService service;
   private transient MixinEnvironment env;
   private transient String name;
   @SerializedName("plugin")
   private String pluginClassName;
   @SerializedName("injectors")
   private MixinConfig.InjectorOptions injectorOptions;
   @SerializedName("overwrites")
   private MixinConfig.OverwriteOptions overwriteOptions;
   private transient PluginHandle plugin;
   private transient IReferenceMapper refMapper;
   private transient boolean initialised = false;
   private transient boolean prepared = false;
   private transient boolean visited = false;

   private MixinConfig() {
   }

   private boolean onLoad(IMixinService service, String name, MixinEnvironment fallbackEnvironment) {
      this.service = service;
      this.name = name;
      if (!Strings.isNullOrEmpty(this.parentName)) {
         return true;
      } else {
         this.env = this.parseSelector(this.selector, fallbackEnvironment);
         this.required = this.requiredValue != null && this.requiredValue && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
         this.initPriority(1000, 1000);
         if (this.injectorOptions == null) {
            this.injectorOptions = new MixinConfig.InjectorOptions();
         }

         if (this.overwriteOptions == null) {
            this.overwriteOptions = new MixinConfig.OverwriteOptions();
         }

         return this.postInit();
      }
   }

   String getParentName() {
      return this.parentName;
   }

   boolean assignParent(Config parentConfig) {
      if (this.parent != null) {
         throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised");
      } else if (parentConfig.get() == this) {
         throw new MixinInitialisationError("Mixin config " + this.name + " cannot be its own parent");
      } else {
         this.parent = parentConfig.get();
         if (!this.parent.initialised) {
            throw new MixinInitialisationError(
               "Mixin config "
                  + this.name
                  + " attempted to assign uninitialised parent config. This probably means that there is an indirect loop in the mixin configs: child -> parent -> child"
            );
         } else {
            this.env = this.parseSelector(this.selector, this.parent.env);
            this.required = this.requiredValue == null
               ? this.parent.required
               : this.requiredValue && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
            this.initPriority(this.parent.priority, this.parent.mixinPriority);
            if (this.injectorOptions == null) {
               this.injectorOptions = this.parent.injectorOptions;
            } else {
               this.injectorOptions.mergeFrom(this.parent.injectorOptions);
            }

            if (this.overwriteOptions == null) {
               this.overwriteOptions = this.parent.overwriteOptions;
            } else {
               this.overwriteOptions.mergeFrom(this.parent.overwriteOptions);
            }

            this.setSourceFile = this.setSourceFile | this.parent.setSourceFile;
            this.verboseLogging = this.verboseLogging | this.parent.verboseLogging;
            return this.postInit();
         }
      }
   }

   private void initPriority(int defaultPriority, int defaultMixinPriority) {
      if (this.priority < 0) {
         this.priority = defaultPriority;
      }

      if (this.mixinPriority < 0) {
         this.mixinPriority = defaultMixinPriority;
      }
   }

   private boolean postInit() throws MixinInitialisationError {
      if (this.initialised) {
         throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised.");
      } else {
         this.initialised = true;
         this.initCompatibilityLevel();
         this.initInjectionPoints();
         return this.checkVersion();
      }
   }

   private void initCompatibilityLevel() {
      if (this.compatibility != null) {
         MixinEnvironment.CompatibilityLevel level = MixinEnvironment.CompatibilityLevel.valueOf(this.compatibility.trim().toUpperCase(Locale.ROOT));
         MixinEnvironment.CompatibilityLevel current = MixinEnvironment.getCompatibilityLevel();
         if (level != current) {
            if (current.isAtLeast(level) && !current.canSupport(level)) {
               throw new MixinInitialisationError("Mixin config " + this.name + " requires compatibility level " + level + " which is too old");
            } else if (!current.canElevateTo(level)) {
               throw new MixinInitialisationError("Mixin config " + this.name + " requires compatibility level " + level + " which is prohibited by " + current);
            } else {
               MixinEnvironment.setCompatibilityLevel(level);
            }
         }
      }
   }

   private MixinEnvironment parseSelector(String target, MixinEnvironment fallbackEnvironment) {
      if (target != null) {
         String[] selectors = target.split("[&\\| ]");

         for (String sel : selectors) {
            sel = sel.trim();
            Pattern environmentSelector = Pattern.compile("^@env(?:ironment)?\\(([A-Z]+)\\)$");
            Matcher environmentSelectorMatcher = environmentSelector.matcher(sel);
            if (environmentSelectorMatcher.matches()) {
               return MixinEnvironment.getEnvironment(MixinEnvironment.Phase.forName(environmentSelectorMatcher.group(1)));
            }
         }

         MixinEnvironment.Phase phase = MixinEnvironment.Phase.forName(target);
         if (phase != null) {
            return MixinEnvironment.getEnvironment(phase);
         }
      }

      return fallbackEnvironment;
   }

   private void initInjectionPoints() {
      if (this.injectorOptions.injectionPoints != null) {
         for (String injectionPointClassName : this.injectorOptions.injectionPoints) {
            this.initInjectionPoint(injectionPointClassName);
         }
      }
   }

   public void initInjectionPoint(String className) {
      try {
         Class<?> injectionPointClass = null;

         try {
            injectionPointClass = this.service.getClassProvider().findClass(className, true);
         } catch (ClassNotFoundException var5) {
            this.logger.error("Unable to register injection point {} for {}, the specified class was not found", new Object[]{className, this, var5});
            return;
         }

         if (!InjectionPoint.class.isAssignableFrom(injectionPointClass)) {
            this.logger.error("Unable to register injection point {} for {}, class must extend InjectionPoint", new Object[]{className, this});
            return;
         }

         try {
            injectionPointClass.getDeclaredMethod("find", String.class, InsnList.class, Collection.class);
         } catch (NoSuchMethodException var4) {
            this.logger
               .error(
                  "Unable to register injection point {} for {}, the class is not compatible with this version of Mixin", new Object[]{className, this, var4}
               );
            return;
         }

         InjectionPoint.register((Class<? extends InjectionPoint>)injectionPointClass);
      } catch (Throwable var6) {
         this.logger.catching(var6);
      }
   }

   private boolean checkVersion() throws MixinInitialisationError {
      if (this.version == null) {
         if (this.parent != null && this.parent.version != null) {
            return true;
         }

         this.logger.error("Mixin config {} does not specify \"minVersion\" property", new Object[]{this.name});
      }

      VersionNumber minVersion = VersionNumber.parse(this.version);
      VersionNumber curVersion = VersionNumber.parse(this.env.getVersion());
      if (minVersion.compareTo(curVersion) > 0) {
         this.logger
            .warn(
               "Mixin config {} requires mixin subsystem version {} but {} was found. The mixin config will not be applied.",
               new Object[]{this.name, minVersion, curVersion}
            );
         if (this.required) {
            throw new MixinInitialisationError("Required mixin config " + this.name + " requires mixin subsystem version " + minVersion);
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   void addListener(MixinConfig.IListener listener) {
      this.listeners.add(listener);
   }

   void onSelect() {
      this.plugin = new PluginHandle(this, this.service, this.pluginClassName);
      this.plugin.onLoad(this.mixinPackage);
      if (!this.mixinPackage.endsWith(".")) {
         this.mixinPackage = this.mixinPackage + ".";
      }

      boolean suppressRefMapWarning = false;
      if (this.refMapperConfig == null) {
         this.refMapperConfig = this.plugin.getRefMapperConfig();
         if (this.refMapperConfig == null) {
            suppressRefMapWarning = true;
            this.refMapperConfig = "mixin.refmap.json";
         }
      }

      this.refMapper = ReferenceMapper.read(this.refMapperConfig);
      this.verboseLogging = this.verboseLogging | this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
      if (!suppressRefMapWarning && this.refMapper.isDefault() && !this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
         this.logger
            .warn(
               "Reference map '{}' for {} could not be read. If this is a development environment you can ignore this message",
               new Object[]{this.refMapperConfig, this}
            );
      }

      if (this.env.getOption(MixinEnvironment.Option.REFMAP_REMAP)) {
         this.refMapper = RemappingReferenceMapper.of(this.env, this.refMapper);
      }
   }

   void prepare() {
      if (!this.prepared) {
         this.prepared = true;
         this.prepareMixins(this.mixinClasses, false);
         switch (this.env.getSide()) {
            case CLIENT:
               this.prepareMixins(this.mixinClassesClient, false);
               break;
            case SERVER:
               this.prepareMixins(this.mixinClassesServer, false);
               break;
            case UNKNOWN:
            default:
               this.logger.warn("Mixin environment was unable to detect the current side, sided mixins will not be applied");
         }
      }
   }

   void postInitialise() {
      if (this.plugin != null) {
         List<String> pluginMixins = this.plugin.getMixins();
         this.prepareMixins(pluginMixins, true);
      }

      Iterator<MixinInfo> iter = this.mixins.iterator();

      while (iter.hasNext()) {
         MixinInfo mixin = iter.next();

         try {
            mixin.validate();

            for (MixinConfig.IListener listener : this.listeners) {
               listener.onInit(mixin);
            }
         } catch (InvalidMixinException var5) {
            this.logger.error(var5.getMixin() + ": " + var5.getMessage(), var5);
            this.removeMixin(mixin);
            iter.remove();
         } catch (Exception var6) {
            this.logger.error(var6.getMessage(), var6);
            this.removeMixin(mixin);
            iter.remove();
         }
      }
   }

   private void removeMixin(MixinInfo remove) {
      for (List<MixinInfo> mixinsFor : this.mixinMapping.values()) {
         Iterator<MixinInfo> iter = mixinsFor.iterator();

         while (iter.hasNext()) {
            if (remove == iter.next()) {
               iter.remove();
            }
         }
      }
   }

   private void prepareMixins(List<String> mixinClasses, boolean ignorePlugin) {
      if (mixinClasses != null) {
         for (String mixinClass : mixinClasses) {
            String fqMixinClass = this.mixinPackage + mixinClass;
            if (mixinClass != null && !globalMixinList.contains(fqMixinClass)) {
               MixinInfo mixin = null;

               try {
                  this.pendingMixins.add(mixin = new MixinInfo(this.service, this, mixinClass, this.plugin, ignorePlugin));
                  globalMixinList.add(fqMixinClass);
               } catch (InvalidMixinException var10) {
                  if (this.required) {
                     throw var10;
                  }

                  this.logger.error(var10.getMessage(), var10);
               } catch (Exception var11) {
                  if (this.required) {
                     throw new InvalidMixinException(mixin, "Error initialising mixin " + mixin + " - " + var11.getClass() + ": " + var11.getMessage(), var11);
                  }

                  this.logger.error(var11.getMessage(), var11);
               }
            }
         }

         for (MixinInfo mixin : this.pendingMixins) {
            try {
               mixin.parseTargets();
               if (mixin.getTargetClasses().size() > 0) {
                  for (String targetClass : mixin.getTargetClasses()) {
                     String targetClassName = targetClass.replace('/', '.');
                     this.mixinsFor(targetClassName).add(mixin);
                     this.unhandledTargets.add(targetClassName);
                  }

                  for (MixinConfig.IListener listener : this.listeners) {
                     listener.onPrepare(mixin);
                  }

                  this.mixins.add(mixin);
               }
            } catch (InvalidMixinException var8) {
               if (this.required) {
                  throw var8;
               }

               this.logger.error(var8.getMessage(), var8);
            } catch (Exception var9) {
               if (this.required) {
                  throw new InvalidMixinException(mixin, "Error initialising mixin " + mixin + " - " + var9.getClass() + ": " + var9.getMessage(), var9);
               }

               this.logger.error(var9.getMessage(), var9);
            }
         }

         this.pendingMixins.clear();
      }
   }

   void postApply(String transformedName, ClassNode targetClass) {
      this.unhandledTargets.remove(transformedName);
   }

   public Config getHandle() {
      if (this.handle == null) {
         this.handle = new Config(this);
      }

      return this.handle;
   }

   @Override
   public boolean isRequired() {
      return this.required;
   }

   @Override
   public MixinEnvironment getEnvironment() {
      return this.env;
   }

   MixinConfig getParent() {
      return this.parent;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getMixinPackage() {
      return this.mixinPackage;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   public int getDefaultMixinPriority() {
      return this.mixinPriority;
   }

   public int getDefaultRequiredInjections() {
      return this.injectorOptions.defaultRequireValue;
   }

   public String getDefaultInjectorGroup() {
      String defaultGroup = this.injectorOptions.defaultGroup;
      return defaultGroup != null && !defaultGroup.isEmpty() ? defaultGroup : "default";
   }

   public boolean conformOverwriteVisibility() {
      return this.overwriteOptions.conformAccessModifiers;
   }

   public boolean requireOverwriteAnnotations() {
      return this.overwriteOptions.requireOverwriteAnnotations;
   }

   public int getMaxShiftByValue() {
      return Math.min(Math.max(this.injectorOptions.maxShiftBy, 0), 5);
   }

   public boolean select(MixinEnvironment environment) {
      this.visited = true;
      return this.env == environment;
   }

   boolean isVisited() {
      return this.visited;
   }

   int getDeclaredMixinCount() {
      return getCollectionSize(this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer);
   }

   int getMixinCount() {
      return this.mixins.size();
   }

   public List<String> getClasses() {
      Builder<String> list = ImmutableList.builder();

      for (List<String> classes : new List[]{this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer}) {
         if (classes != null) {
            for (String className : classes) {
               list.add(this.mixinPackage + className);
            }
         }
      }

      return list.build();
   }

   public boolean shouldSetSourceFile() {
      return this.setSourceFile;
   }

   public IReferenceMapper getReferenceMapper() {
      if (this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
         return ReferenceMapper.DEFAULT_MAPPER;
      } else {
         this.refMapper.setContext(this.env.getRefmapObfuscationContext());
         return this.refMapper;
      }
   }

   String remapClassName(String className, String reference) {
      return this.getReferenceMapper().remap(className, reference);
   }

   @Override
   public IMixinConfigPlugin getPlugin() {
      return this.plugin.get();
   }

   @Override
   public Set<String> getTargets() {
      return Collections.unmodifiableSet(this.mixinMapping.keySet());
   }

   public Set<String> getUnhandledTargets() {
      return Collections.unmodifiableSet(this.unhandledTargets);
   }

   public Level getLoggingLevel() {
      return this.verboseLogging ? Level.INFO : Level.DEBUG;
   }

   public boolean packageMatch(String className) {
      return className.startsWith(this.mixinPackage);
   }

   public boolean hasMixinsFor(String targetClass) {
      return this.mixinMapping.containsKey(targetClass);
   }

   boolean hasPendingMixinsFor(String targetClass) {
      if (this.packageMatch(targetClass)) {
         return false;
      } else {
         for (MixinInfo pendingMixin : this.pendingMixins) {
            if (pendingMixin.hasDeclaredTarget(targetClass)) {
               return true;
            }
         }

         return false;
      }
   }

   public List<MixinInfo> getMixinsFor(String targetClass) {
      return this.mixinsFor(targetClass);
   }

   private List<MixinInfo> mixinsFor(String targetClass) {
      List<MixinInfo> mixins = this.mixinMapping.get(targetClass);
      if (mixins == null) {
         mixins = new ArrayList<>();
         this.mixinMapping.put(targetClass, mixins);
      }

      return mixins;
   }

   public List<String> reloadMixin(String mixinClass, ClassNode classNode) {
      for (MixinInfo mixin : this.mixins) {
         if (mixin.getClassName().equals(mixinClass)) {
            mixin.reloadMixin(classNode);
            return mixin.getTargetClasses();
         }
      }

      return Collections.emptyList();
   }

   @Override
   public String toString() {
      return this.name;
   }

   public int compareTo(MixinConfig other) {
      if (other == null) {
         return 0;
      } else {
         return other.priority == this.priority ? this.order - other.order : this.priority - other.priority;
      }
   }

   static Config create(String configFile, MixinEnvironment outer) {
      try {
         IMixinService service = MixinService.getService();
         InputStream resource = service.getResourceAsStream(configFile);
         if (resource == null) {
            throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile));
         } else {
            MixinConfig config = (MixinConfig)new Gson().fromJson(new InputStreamReader(resource), MixinConfig.class);
            return config.onLoad(service, configFile, outer) ? config.getHandle() : null;
         }
      } catch (IllegalArgumentException var5) {
         throw var5;
      } catch (Exception var6) {
         throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile), var6);
      }
   }

   private static int getCollectionSize(Collection<?>... collections) {
      int total = 0;

      for (Collection<?> collection : collections) {
         if (collection != null) {
            total += collection.size();
         }
      }

      return total;
   }

   interface IListener {
      void onPrepare(MixinInfo var1);

      void onInit(MixinInfo var1);
   }

   static class InjectorOptions {
      @SerializedName("defaultRequire")
      int defaultRequireValue = 0;
      @SerializedName("defaultGroup")
      String defaultGroup = "default";
      @SerializedName("injectionPoints")
      List<String> injectionPoints;
      @SerializedName("maxShiftBy")
      int maxShiftBy = 0;

      InjectorOptions() {
      }

      void mergeFrom(MixinConfig.InjectorOptions parent) {
         if (this.defaultRequireValue == 0) {
            this.defaultRequireValue = parent.defaultRequireValue;
         }

         if ("default".equals(this.defaultGroup)) {
            this.defaultGroup = parent.defaultGroup;
         }

         if (this.maxShiftBy == 0) {
            this.maxShiftBy = parent.maxShiftBy;
         }
      }
   }

   static class OverwriteOptions {
      @SerializedName("conformVisibility")
      boolean conformAccessModifiers;
      @SerializedName("requireAnnotations")
      boolean requireOverwriteAnnotations;

      OverwriteOptions() {
      }

      void mergeFrom(MixinConfig.OverwriteOptions parent) {
         this.conformAccessModifiers = this.conformAccessModifiers | parent.conformAccessModifiers;
         this.requireOverwriteAnnotations = this.requireOverwriteAnnotations | parent.requireOverwriteAnnotations;
      }
   }
}
