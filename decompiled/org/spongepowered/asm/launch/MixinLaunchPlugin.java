package org.spongepowered.asm.launch;

import com.google.common.io.Resources;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.ITransformerLoader;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;
import org.spongepowered.asm.service.modlauncher.ModLauncherAuditTrail;

public class MixinLaunchPlugin implements ILaunchPluginService, IClassBytecodeProvider {
   public static final String NAME = "mixin";
   private final List<IClassProcessor> processors = new ArrayList<>();
   private List<String> commandLineMixins;
   private ITransformerLoader transformerLoader;
   private MixinServiceModLauncher service;
   private ModLauncherAuditTrail auditTrail;

   public MixinLaunchPlugin() {
   }

   public String name() {
      return "mixin";
   }

   public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
      throw new IllegalStateException("Outdated ModLauncher");
   }

   public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
      throw new IllegalStateException("Outdated ModLauncher");
   }

   public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
      if ("mixin".equals(reason)) {
         return Phases.NONE;
      } else {
         EnumSet<Phase> phases = EnumSet.noneOf(Phase.class);
         synchronized (this.processors) {
            for (IClassProcessor postProcessor : this.processors) {
               EnumSet<Phase> processorVote = postProcessor.handlesClass(classType, isEmpty, reason);
               if (processorVote != null) {
                  phases.addAll(processorVote);
               }
            }

            return phases;
         }
      }
   }

   public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
      boolean processed = false;
      synchronized (this.processors) {
         for (IClassProcessor postProcessor : this.processors) {
            processed |= postProcessor.processClass(phase, classNode, classType, reason);
         }

         return processed;
      }
   }

   void init(IEnvironment environment, List<String> commandLineMixins) {
      IMixinService service = MixinService.getService();
      if (!(service instanceof MixinServiceModLauncher)) {
         throw new IllegalStateException("Unsupported service type for ModLauncher Mixin Service");
      } else {
         this.service = (MixinServiceModLauncher)service;
         this.auditTrail = (ModLauncherAuditTrail)this.service.getAuditTrail();
         synchronized (this.processors) {
            this.processors.addAll(this.service.getProcessors());
         }

         this.commandLineMixins = commandLineMixins;
         this.service.onInit(this);
      }
   }

   public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
      if (this.auditTrail != null) {
         this.auditTrail.setConsumer(className, auditDataAcceptor);
      }
   }

   @Deprecated
   public void addResource(Path resource, String name) {
      this.service.getPrimaryContainer().addResource(name, resource);
   }

   public void offerResource(Path resource, String name) {
      this.service.getPrimaryContainer().addResource(name, resource);
   }

   public void addResources(List<Entry<String, Path>> resources) {
      this.service.getPrimaryContainer().addResources(resources);
   }

   public <T> T getExtension() {
      return null;
   }

   public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
      this.transformerLoader = transformerLoader;
      MixinBootstrap.doInit(CommandLineOptions.of(this.commandLineMixins));
      MixinBootstrap.inject();
      this.service.onStartup();
   }

   @Override
   public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, true);
   }

   @Override
   public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      if (!runTransformers) {
         throw new IllegalArgumentException("ModLauncher service does not currently support retrieval of untransformed bytecode");
      } else {
         byte[] classBytes;
         try {
            classBytes = this.transformerLoader.buildTransformedClassNodeFor(name);
         } catch (ClassNotFoundException var8) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(name.replace('.', '/') + ".class");
            if (url == null) {
               throw var8;
            }

            try {
               classBytes = Resources.asByteSource(url).read();
            } catch (IOException var7) {
               throw var8;
            }
         }

         if (classBytes == null) {
            throw new ClassNotFoundException(name.replace('/', '.'));
         } else {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(classNode, 8);
            return classNode;
         }
      }
   }
}
