package org.spongepowered.asm.mixin;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.obfuscation.RemapperChain;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.perf.Profiler;

public final class MixinEnvironment implements ITokenProvider {
   private static MixinEnvironment currentEnvironment;
   private static MixinEnvironment.Phase currentPhase = MixinEnvironment.Phase.NOT_INITIALISED;
   private static MixinEnvironment.CompatibilityLevel compatibility;
   private static boolean showHeader = true;
   private static final Logger logger = LogManager.getLogger("mixin");
   private static final Profiler profiler = new Profiler();
   private static IMixinTransformer transformer;
   private final IMixinService service;
   private final MixinEnvironment.Phase phase;
   private final GlobalProperties.Keys configsKey;
   private final boolean[] options;
   private final Set<String> tokenProviderClasses = new HashSet<>();
   private final List<MixinEnvironment.TokenProviderWrapper> tokenProviders = new ArrayList<>();
   private final Map<String, Integer> internalTokens = new HashMap<>();
   private final RemapperChain remappers = new RemapperChain();
   private MixinEnvironment.Side side;
   private String obfuscationContext = null;

   MixinEnvironment(MixinEnvironment.Phase phase) {
      this.service = MixinService.getService();
      this.phase = phase;
      this.configsKey = GlobalProperties.Keys.of(GlobalProperties.Keys.CONFIGS + "." + this.phase.name.toLowerCase(Locale.ROOT));
      Object version = this.getVersion();
      if (version != null && "0.8.1".equals(version)) {
         this.service.checkEnv(this);
         this.options = new boolean[MixinEnvironment.Option.values().length];

         for (MixinEnvironment.Option option : MixinEnvironment.Option.values()) {
            this.options[option.ordinal()] = option.getBooleanValue();
         }

         if (showHeader) {
            showHeader = false;
            this.printHeader(version);
         }
      } else {
         throw new MixinException("Environment conflict, mismatched versions or you didn't call MixinBootstrap.init()");
      }
   }

   private void printHeader(Object version) {
      String codeSource = this.getCodeSource();
      String serviceName = this.service.getName();
      MixinEnvironment.Side side = this.getSide();
      logger.info("SpongePowered MIXIN Subsystem Version={} Source={} Service={} Env={}", new Object[]{version, codeSource, serviceName, side});
      boolean verbose = this.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
      if (verbose || this.getOption(MixinEnvironment.Option.DEBUG_EXPORT) || this.getOption(MixinEnvironment.Option.DEBUG_PROFILER)) {
         PrettyPrinter printer = new PrettyPrinter(32);
         printer.add("SpongePowered MIXIN%s", verbose ? " (Verbose debugging enabled)" : "").centre().hr();
         printer.kv("Code source", codeSource);
         printer.kv("Internal Version", version);
         printer.kv("Java Versions Supported", MixinEnvironment.CompatibilityLevel.getSupportedVersions());
         printer.kv("Current Compatibility Level", getCompatibilityLevel());
         printer.kv("Detected ASM API Version", ASM.getApiVersionString()).hr();
         printer.kv("Service Name", serviceName);
         printer.kv("Mixin Service Class", this.service.getClass().getName());
         printer.kv("Global Property Service Class", MixinService.getGlobalPropertyService().getClass().getName()).hr();

         for (MixinEnvironment.Option option : MixinEnvironment.Option.values()) {
            StringBuilder indent = new StringBuilder();

            for (int i = 0; i < option.depth; i++) {
               indent.append("- ");
            }

            printer.kv(option.property, "%s<%s>", indent, option);
         }

         printer.hr().kv("Detected Side", side);
         printer.print(System.err);
      }
   }

   private String getCodeSource() {
      try {
         return this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
      } catch (Throwable var2) {
         return "Unknown";
      }
   }

   private Level getVerboseLoggingLevel() {
      return this.getOption(MixinEnvironment.Option.DEBUG_VERBOSE) ? Level.INFO : Level.DEBUG;
   }

   public MixinEnvironment.Phase getPhase() {
      return this.phase;
   }

   @Deprecated
   public List<String> getMixinConfigs() {
      List<String> mixinConfigs = GlobalProperties.get(this.configsKey);
      if (mixinConfigs == null) {
         mixinConfigs = new ArrayList<>();
         GlobalProperties.put(this.configsKey, mixinConfigs);
      }

      return mixinConfigs;
   }

   @Deprecated
   public MixinEnvironment addConfiguration(String config) {
      logger.warn("MixinEnvironment::addConfiguration is deprecated and will be removed. Use Mixins::addConfiguration instead!");
      Mixins.addConfiguration(config, this);
      return this;
   }

   void registerConfig(String config) {
      List<String> configs = this.getMixinConfigs();
      if (!configs.contains(config)) {
         configs.add(config);
      }
   }

   public MixinEnvironment registerTokenProviderClass(String providerName) {
      if (!this.tokenProviderClasses.contains(providerName)) {
         try {
            Class<? extends IEnvironmentTokenProvider> providerClass = (Class<? extends IEnvironmentTokenProvider>)this.service
               .getClassProvider()
               .findClass(providerName, true);
            IEnvironmentTokenProvider provider = providerClass.newInstance();
            this.registerTokenProvider(provider);
         } catch (Throwable var4) {
            logger.error("Error instantiating " + providerName, var4);
         }
      }

      return this;
   }

   public MixinEnvironment registerTokenProvider(IEnvironmentTokenProvider provider) {
      if (provider != null && !this.tokenProviderClasses.contains(provider.getClass().getName())) {
         String providerName = provider.getClass().getName();
         MixinEnvironment.TokenProviderWrapper wrapper = new MixinEnvironment.TokenProviderWrapper(provider, this);
         logger.log(this.getVerboseLoggingLevel(), "Adding new token provider {} to {}", new Object[]{providerName, this});
         this.tokenProviders.add(wrapper);
         this.tokenProviderClasses.add(providerName);
         Collections.sort(this.tokenProviders);
      }

      return this;
   }

   @Override
   public Integer getToken(String token) {
      token = token.toUpperCase(Locale.ROOT);

      for (MixinEnvironment.TokenProviderWrapper provider : this.tokenProviders) {
         Integer value = provider.getToken(token);
         if (value != null) {
            return value;
         }
      }

      return this.internalTokens.get(token);
   }

   @Deprecated
   public Set<String> getErrorHandlerClasses() {
      return Mixins.getErrorHandlerClasses();
   }

   public Object getActiveTransformer() {
      return transformer;
   }

   public void setActiveTransformer(IMixinTransformer transformer) {
      if (transformer != null) {
         MixinEnvironment.transformer = transformer;
      }
   }

   public MixinEnvironment setSide(MixinEnvironment.Side side) {
      if (side != null && this.getSide() == MixinEnvironment.Side.UNKNOWN && side != MixinEnvironment.Side.UNKNOWN) {
         this.side = side;
      }

      return this;
   }

   public MixinEnvironment.Side getSide() {
      if (this.side == null) {
         for (MixinEnvironment.Side side : MixinEnvironment.Side.values()) {
            if (side.detect()) {
               this.side = side;
               break;
            }
         }
      }

      return this.side != null ? this.side : MixinEnvironment.Side.UNKNOWN;
   }

   public String getVersion() {
      return GlobalProperties.get(GlobalProperties.Keys.INIT);
   }

   public boolean getOption(MixinEnvironment.Option option) {
      return this.options[option.ordinal()];
   }

   public void setOption(MixinEnvironment.Option option, boolean value) {
      this.options[option.ordinal()] = value;
   }

   public String getOptionValue(MixinEnvironment.Option option) {
      return option.getStringValue();
   }

   public <E extends Enum<E>> E getOption(MixinEnvironment.Option option, E defaultValue) {
      return option.getEnumValue(defaultValue);
   }

   public void setObfuscationContext(String context) {
      this.obfuscationContext = context;
   }

   public String getObfuscationContext() {
      return this.obfuscationContext;
   }

   public String getRefmapObfuscationContext() {
      String overrideObfuscationType = MixinEnvironment.Option.OBFUSCATION_TYPE.getStringValue();
      return overrideObfuscationType != null ? overrideObfuscationType : this.obfuscationContext;
   }

   public RemapperChain getRemappers() {
      return this.remappers;
   }

   public void audit() {
      Object activeTransformer = this.getActiveTransformer();
      if (activeTransformer instanceof IMixinTransformer) {
         ((IMixinTransformer)activeTransformer).audit(this);
      }
   }

   @Deprecated
   public List<ITransformer> getTransformers() {
      logger.warn("MixinEnvironment::getTransformers is deprecated!");
      ITransformerProvider transformers = this.service.getTransformerProvider();
      return transformers != null ? (List)transformers.getTransformers() : Collections.emptyList();
   }

   @Deprecated
   public void addTransformerExclusion(String name) {
      logger.warn("MixinEnvironment::addTransformerExclusion is deprecated!");
      ITransformerProvider transformers = this.service.getTransformerProvider();
      if (transformers != null) {
         transformers.addTransformerExclusion(name);
      }
   }

   @Override
   public String toString() {
      return String.format("%s[%s]", this.getClass().getSimpleName(), this.phase);
   }

   private static MixinEnvironment.Phase getCurrentPhase() {
      if (currentPhase == MixinEnvironment.Phase.NOT_INITIALISED) {
         init(MixinEnvironment.Phase.PREINIT);
      }

      return currentPhase;
   }

   public static void init(MixinEnvironment.Phase phase) {
      if (currentPhase == MixinEnvironment.Phase.NOT_INITIALISED) {
         currentPhase = phase;
         MixinEnvironment env = getEnvironment(phase);
         getProfiler().setActive(env.getOption(MixinEnvironment.Option.DEBUG_PROFILER));
         IMixinService service = MixinService.getService();
         if (service instanceof MixinServiceAbstract) {
            ((MixinServiceAbstract)service).wire(phase, new MixinEnvironment.PhaseConsumer());
         }
      }
   }

   public static MixinEnvironment getEnvironment(MixinEnvironment.Phase phase) {
      return phase == null ? MixinEnvironment.Phase.DEFAULT.getEnvironment() : phase.getEnvironment();
   }

   public static MixinEnvironment getDefaultEnvironment() {
      return getEnvironment(MixinEnvironment.Phase.DEFAULT);
   }

   public static MixinEnvironment getCurrentEnvironment() {
      if (currentEnvironment == null) {
         currentEnvironment = getEnvironment(getCurrentPhase());
      }

      return currentEnvironment;
   }

   public static MixinEnvironment.CompatibilityLevel getCompatibilityLevel() {
      if (compatibility == null) {
         MixinEnvironment.CompatibilityLevel minLevel = getMinCompatibilityLevel();
         MixinEnvironment.CompatibilityLevel optionLevel = MixinEnvironment.Option.DEFAULT_COMPATIBILITY_LEVEL.getEnumValue(minLevel);
         compatibility = optionLevel.isAtLeast(minLevel) ? optionLevel : minLevel;
      }

      return compatibility;
   }

   private static MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
      MixinEnvironment.CompatibilityLevel minLevel = MixinService.getService().getMinCompatibilityLevel();
      return minLevel == null ? MixinEnvironment.CompatibilityLevel.DEFAULT : minLevel;
   }

   @Deprecated
   public static void setCompatibilityLevel(MixinEnvironment.CompatibilityLevel level) throws IllegalArgumentException {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      if (!"org.spongepowered.asm.mixin.transformer.MixinConfig".equals(stackTrace[2].getClassName())) {
         logger.warn("MixinEnvironment::setCompatibilityLevel is deprecated and will be removed. Set level via config instead!");
      }

      MixinEnvironment.CompatibilityLevel currentLevel = getCompatibilityLevel();
      if (level != currentLevel && level.isAtLeast(currentLevel)) {
         if (!level.isSupported()) {
            throw new IllegalArgumentException("The requested compatibility level " + level + " could not be set. Level is not supported");
         }

         IMixinService service = MixinService.getService();
         MixinEnvironment.CompatibilityLevel maxLevel = service.getMaxCompatibilityLevel();
         if (maxLevel != null && maxLevel.isLessThan(level)) {
            logger.warn(
               "The requested compatibility level {} is higher than the level supported by the active subsystem '{}' which supports {}. This is not a supported configuration and instability may occur.",
               new Object[]{level, service.getName(), maxLevel}
            );
         }

         compatibility = level;
         logger.info("Compatibility level set to {}", new Object[]{level});
      }
   }

   public static Profiler getProfiler() {
      return profiler;
   }

   static void gotoPhase(MixinEnvironment.Phase phase) {
      if (phase != null && phase.ordinal >= 0) {
         IMixinService service = MixinService.getService();
         if (phase.ordinal > getCurrentPhase().ordinal) {
            service.beginPhase();
         }

         currentPhase = phase;
         currentEnvironment = getEnvironment(getCurrentPhase());
         if (service instanceof MixinServiceAbstract && phase == MixinEnvironment.Phase.DEFAULT) {
            ((MixinServiceAbstract)service).unwire();
         }
      } else {
         throw new IllegalArgumentException("Cannot go to the specified phase, phase is null or invalid");
      }
   }

   public static enum CompatibilityLevel {
      JAVA_6(6, 50, 0),
      JAVA_7(7, 51, 0) {
         @Override
         boolean isSupported() {
            return JavaVersion.current() >= 1.7;
         }
      },
      JAVA_8(8, 52, 1) {
         @Override
         boolean isSupported() {
            return JavaVersion.current() >= 1.8;
         }
      },
      JAVA_9(9, 53, 3) {
         @Override
         boolean isSupported() {
            return JavaVersion.current() >= 9.0;
         }
      },
      JAVA_10(10, 54, 3) {
         @Override
         boolean isSupported() {
            return JavaVersion.current() >= 10.0;
         }
      },
      JAVA_11(11, 55, 15) {
         @Override
         boolean isSupported() {
            return JavaVersion.current() >= 11.0;
         }
      };

      public static MixinEnvironment.CompatibilityLevel DEFAULT = JAVA_6;
      private final int ver;
      private final int classVersion;
      private final int languageFeatures;
      private MixinEnvironment.CompatibilityLevel maxCompatibleLevel;

      private CompatibilityLevel(int ver, int classVersion, int languageFeatures) {
         this.ver = ver;
         this.classVersion = classVersion;
         this.languageFeatures = languageFeatures;
      }

      boolean isSupported() {
         return true;
      }

      public int classVersion() {
         return this.classVersion;
      }

      @Deprecated
      public boolean supportsMethodsInInterfaces() {
         return (this.languageFeatures & 1) != 0;
      }

      public boolean supports(int languageFeature) {
         return (this.languageFeatures & languageFeature) != 0;
      }

      public boolean isAtLeast(MixinEnvironment.CompatibilityLevel level) {
         return level == null || this.ver >= level.ver;
      }

      public boolean isLessThan(MixinEnvironment.CompatibilityLevel level) {
         return level == null || this.ver < level.ver;
      }

      public boolean canElevateTo(MixinEnvironment.CompatibilityLevel level) {
         return level != null && this.maxCompatibleLevel != null ? level.ver <= this.maxCompatibleLevel.ver : true;
      }

      public boolean canSupport(MixinEnvironment.CompatibilityLevel level) {
         return level == null ? true : level.canElevateTo(this);
      }

      static String getSupportedVersions() {
         StringBuilder sb = new StringBuilder();
         boolean comma = false;

         for (MixinEnvironment.CompatibilityLevel level : values()) {
            if (level.isSupported()) {
               if (comma) {
                  sb.append(", ");
               }

               sb.append(level.ver);
               comma = true;
            }
         }

         return sb.toString();
      }

      public static class LanguageFeature {
         public static final int METHODS_IN_INTERFACES = 1;
         public static final int PRIVATE_METHODS_IN_INTERFACES = 2;
         public static final int NESTING = 4;
         public static final int DYNAMIC_CONSTANTS = 8;

         public LanguageFeature() {
         }
      }
   }

   public static enum Option {
      DEBUG_ALL("debug"),
      DEBUG_EXPORT(DEBUG_ALL, "export"),
      DEBUG_EXPORT_FILTER(DEBUG_EXPORT, "filter", false),
      DEBUG_EXPORT_DECOMPILE(DEBUG_EXPORT, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "decompile"),
      DEBUG_EXPORT_DECOMPILE_THREADED(DEBUG_EXPORT_DECOMPILE, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "async"),
      DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES(DEBUG_EXPORT_DECOMPILE, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "mergeGenericSignatures"),
      DEBUG_VERIFY(DEBUG_ALL, "verify"),
      DEBUG_VERBOSE(DEBUG_ALL, "verbose"),
      DEBUG_INJECTORS(DEBUG_ALL, "countInjections"),
      DEBUG_STRICT(DEBUG_ALL, MixinEnvironment.Option.Inherit.INDEPENDENT, "strict"),
      DEBUG_UNIQUE(DEBUG_STRICT, "unique"),
      DEBUG_TARGETS(DEBUG_STRICT, "targets"),
      DEBUG_PROFILER(DEBUG_ALL, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "profiler"),
      DUMP_TARGET_ON_FAILURE("dumpTargetOnFailure"),
      CHECK_ALL("checks"),
      CHECK_IMPLEMENTS(CHECK_ALL, "interfaces"),
      CHECK_IMPLEMENTS_STRICT(CHECK_IMPLEMENTS, MixinEnvironment.Option.Inherit.ALLOW_OVERRIDE, "strict"),
      IGNORE_CONSTRAINTS("ignoreConstraints"),
      HOT_SWAP("hotSwap"),
      ENVIRONMENT(MixinEnvironment.Option.Inherit.ALWAYS_FALSE, "env"),
      OBFUSCATION_TYPE(ENVIRONMENT, MixinEnvironment.Option.Inherit.ALWAYS_FALSE, "obf"),
      DISABLE_REFMAP(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "disableRefMap"),
      REFMAP_REMAP(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "remapRefMap"),
      REFMAP_REMAP_RESOURCE(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "refMapRemappingFile", ""),
      REFMAP_REMAP_SOURCE_ENV(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "refMapRemappingEnv", "searge"),
      REFMAP_REMAP_ALLOW_PERMISSIVE(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "allowPermissiveMatch", true, "true"),
      IGNORE_REQUIRED(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "ignoreRequired"),
      DEFAULT_COMPATIBILITY_LEVEL(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "compatLevel"),
      SHIFT_BY_VIOLATION_BEHAVIOUR(ENVIRONMENT, MixinEnvironment.Option.Inherit.INDEPENDENT, "shiftByViolation", "warn"),
      INITIALISER_INJECTION_MODE("initialiserInjectionMode", "default");

      private static final String PREFIX = "mixin";
      final MixinEnvironment.Option parent;
      final MixinEnvironment.Option.Inherit inheritance;
      final String property;
      final String defaultValue;
      final boolean isFlag;
      final int depth;

      private Option(String property) {
         this(null, property, true);
      }

      private Option(MixinEnvironment.Option.Inherit inheritance, String property) {
         this(null, inheritance, property, true);
      }

      private Option(String property, boolean flag) {
         this(null, property, flag);
      }

      private Option(String property, String defaultStringValue) {
         this(null, MixinEnvironment.Option.Inherit.INDEPENDENT, property, false, defaultStringValue);
      }

      private Option(MixinEnvironment.Option parent, String property) {
         this(parent, MixinEnvironment.Option.Inherit.INHERIT, property, true);
      }

      private Option(MixinEnvironment.Option parent, MixinEnvironment.Option.Inherit inheritance, String property) {
         this(parent, inheritance, property, true);
      }

      private Option(MixinEnvironment.Option parent, String property, boolean isFlag) {
         this(parent, MixinEnvironment.Option.Inherit.INHERIT, property, isFlag, null);
      }

      private Option(MixinEnvironment.Option parent, MixinEnvironment.Option.Inherit inheritance, String property, boolean isFlag) {
         this(parent, inheritance, property, isFlag, null);
      }

      private Option(MixinEnvironment.Option parent, String property, String defaultStringValue) {
         this(parent, MixinEnvironment.Option.Inherit.INHERIT, property, false, defaultStringValue);
      }

      private Option(MixinEnvironment.Option parent, MixinEnvironment.Option.Inherit inheritance, String property, String defaultStringValue) {
         this(parent, inheritance, property, false, defaultStringValue);
      }

      private Option(MixinEnvironment.Option parent, MixinEnvironment.Option.Inherit inheritance, String property, boolean isFlag, String defaultStringValue) {
         this.parent = parent;
         this.inheritance = inheritance;
         this.property = (parent != null ? parent.property : "mixin") + "." + property;
         this.defaultValue = defaultStringValue;
         this.isFlag = isFlag;

         int depth;
         for (depth = 0; parent != null; depth++) {
            parent = parent.parent;
         }

         this.depth = depth;
      }

      MixinEnvironment.Option getParent() {
         return this.parent;
      }

      String getProperty() {
         return this.property;
      }

      @Override
      public String toString() {
         return this.isFlag ? String.valueOf(this.getBooleanValue()) : this.getStringValue();
      }

      private boolean getLocalBooleanValue(boolean defaultValue) {
         return Boolean.parseBoolean(System.getProperty(this.property, Boolean.toString(defaultValue)));
      }

      private boolean getInheritedBooleanValue() {
         return this.parent != null && this.parent.getBooleanValue();
      }

      final boolean getBooleanValue() {
         if (this.inheritance == MixinEnvironment.Option.Inherit.ALWAYS_FALSE) {
            return false;
         } else {
            boolean local = this.getLocalBooleanValue(false);
            if (this.inheritance == MixinEnvironment.Option.Inherit.INDEPENDENT) {
               return local;
            } else {
               boolean inherited = local || this.getInheritedBooleanValue();
               return this.inheritance == MixinEnvironment.Option.Inherit.INHERIT ? inherited : this.getLocalBooleanValue(inherited);
            }
         }
      }

      final String getStringValue() {
         return this.inheritance != MixinEnvironment.Option.Inherit.INDEPENDENT && this.parent != null && !this.parent.getBooleanValue()
            ? this.defaultValue
            : System.getProperty(this.property, this.defaultValue);
      }

      <E extends Enum<E>> E getEnumValue(E defaultValue) {
         String value = System.getProperty(this.property, defaultValue.name());

         try {
            return Enum.valueOf((Class<E>)defaultValue.getClass(), value.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var4) {
            return defaultValue;
         }
      }

      private static enum Inherit {
         INHERIT,
         ALLOW_OVERRIDE,
         INDEPENDENT,
         ALWAYS_FALSE;

         private Inherit() {
         }
      }
   }

   public static final class Phase {
      static final MixinEnvironment.Phase NOT_INITIALISED = new MixinEnvironment.Phase(-1, "NOT_INITIALISED");
      public static final MixinEnvironment.Phase PREINIT = new MixinEnvironment.Phase(0, "PREINIT");
      public static final MixinEnvironment.Phase INIT = new MixinEnvironment.Phase(1, "INIT");
      public static final MixinEnvironment.Phase DEFAULT = new MixinEnvironment.Phase(2, "DEFAULT");
      static final List<MixinEnvironment.Phase> phases = ImmutableList.of(PREINIT, INIT, DEFAULT);
      final int ordinal;
      final String name;
      private MixinEnvironment environment;

      private Phase(int ordinal, String name) {
         this.ordinal = ordinal;
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public static MixinEnvironment.Phase forName(String name) {
         for (MixinEnvironment.Phase phase : phases) {
            if (phase.name.equals(name)) {
               return phase;
            }
         }

         return null;
      }

      MixinEnvironment getEnvironment() {
         if (this.ordinal < 0) {
            throw new IllegalArgumentException("Cannot access the NOT_INITIALISED environment");
         } else {
            if (this.environment == null) {
               this.environment = new MixinEnvironment(this);
            }

            return this.environment;
         }
      }
   }

   static class PhaseConsumer implements IConsumer<MixinEnvironment.Phase> {
      PhaseConsumer() {
      }

      public void accept(MixinEnvironment.Phase phase) {
         MixinEnvironment.gotoPhase(phase);
      }
   }

   public static enum Side {
      UNKNOWN {
         @Override
         protected boolean detect() {
            return false;
         }
      },
      CLIENT {
         @Override
         protected boolean detect() {
            String sideName = MixinService.getService().getSideName();
            return "CLIENT".equals(sideName);
         }
      },
      SERVER {
         @Override
         protected boolean detect() {
            String sideName = MixinService.getService().getSideName();
            return "SERVER".equals(sideName) || "DEDICATEDSERVER".equals(sideName);
         }
      };

      private Side() {
      }

      protected abstract boolean detect();
   }

   static class TokenProviderWrapper implements Comparable<MixinEnvironment.TokenProviderWrapper> {
      private static int nextOrder = 0;
      private final int priority;
      private final int order;
      private final IEnvironmentTokenProvider provider;
      private final MixinEnvironment environment;

      public TokenProviderWrapper(IEnvironmentTokenProvider provider, MixinEnvironment environment) {
         this.provider = provider;
         this.environment = environment;
         this.order = nextOrder++;
         this.priority = provider.getPriority();
      }

      public int compareTo(MixinEnvironment.TokenProviderWrapper other) {
         if (other == null) {
            return 0;
         } else {
            return other.priority == this.priority ? other.order - this.order : other.priority - this.priority;
         }
      }

      public IEnvironmentTokenProvider getProvider() {
         return this.provider;
      }

      Integer getToken(String token) {
         return this.provider.getToken(token, this.environment);
      }
   }
}
