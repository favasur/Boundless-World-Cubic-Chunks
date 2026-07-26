package org.spongepowered.asm.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ReEntranceLock;

public abstract class MixinServiceAbstract implements IMixinService {
   protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
   protected static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
   protected static final Logger logger = LogManager.getLogger("mixin");
   protected final ReEntranceLock lock = new ReEntranceLock(1);
   private List<IMixinPlatformServiceAgent> serviceAgents;
   private String sideName;

   public MixinServiceAbstract() {
   }

   @Override
   public void prepare() {
   }

   @Override
   public MixinEnvironment.Phase getInitialPhase() {
      return MixinEnvironment.Phase.PREINIT;
   }

   @Override
   public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
      return null;
   }

   @Override
   public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
      return null;
   }

   @Override
   public void beginPhase() {
   }

   @Override
   public void checkEnv(Object bootSource) {
   }

   @Override
   public void init() {
      for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.init();
      }
   }

   @Override
   public ReEntranceLock getReEntranceLock() {
      return this.lock;
   }

   @Override
   public Collection<IContainerHandle> getMixinContainers() {
      Builder<IContainerHandle> list = ImmutableList.builder();
      this.getContainersFromAgents(list);
      return list.build();
   }

   protected final void getContainersFromAgents(Builder<IContainerHandle> list) {
      for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         Collection<IContainerHandle> containers = agent.getMixinContainers();
         if (containers != null) {
            list.addAll(containers);
         }
      }
   }

   @Override
   public final String getSideName() {
      if (this.sideName != null) {
         return this.sideName;
      } else {
         for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            try {
               String side = agent.getSideName();
               if (side != null) {
                  return this.sideName = side;
               }
            } catch (Exception var4) {
               logger.catching(var4);
            }
         }

         return "UNKNOWN";
      }
   }

   private List<IMixinPlatformServiceAgent> getServiceAgents() {
      if (this.serviceAgents != null) {
         return this.serviceAgents;
      } else {
         this.serviceAgents = new ArrayList<>();

         for (String agentClassName : this.getPlatformAgents()) {
            try {
               Class<IMixinPlatformAgent> agentClass = (Class<IMixinPlatformAgent>)this.getClassProvider().findClass(agentClassName, false);
               IMixinPlatformAgent agent = agentClass.newInstance();
               if (agent instanceof IMixinPlatformServiceAgent) {
                  this.serviceAgents.add((IMixinPlatformServiceAgent)agent);
               }
            } catch (Exception var5) {
               var5.printStackTrace();
            }
         }

         return this.serviceAgents;
      }
   }

   @Deprecated
   public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
      for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.wire(phase, phaseConsumer);
      }
   }

   @Deprecated
   public void unwire() {
      for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
         agent.unwire();
      }
   }
}
