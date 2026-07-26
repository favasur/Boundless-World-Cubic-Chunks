package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

public class MixinConnectorManager {
   private static final Logger logger = LogManager.getLogger("mixin");
   private final Set<String> connectorClasses = new LinkedHashSet<>();
   private final List<IMixinConnector> connectors = new ArrayList<>();

   MixinConnectorManager() {
   }

   void addConnector(String connectorClass) {
      this.connectorClasses.add(connectorClass);
   }

   void inject() {
      this.loadConnectors();
      this.initConnectors();
   }

   void loadConnectors() {
      IClassProvider classProvider = MixinService.getService().getClassProvider();

      for (String connectorClassName : this.connectorClasses) {
         Class<IMixinConnector> connectorClass = null;

         try {
            Class<?> clazz = classProvider.findClass(connectorClassName);
            if (!IMixinConnector.class.isAssignableFrom(clazz)) {
               logger.error("Mixin Connector [" + connectorClassName + "] does not implement IMixinConnector");
               continue;
            }

            connectorClass = (Class<IMixinConnector>)clazz;
         } catch (ClassNotFoundException var7) {
            logger.catching(var7);
            continue;
         }

         try {
            IMixinConnector connector = connectorClass.newInstance();
            this.connectors.add(connector);
            logger.info("Successfully loaded Mixin Connector [" + connectorClassName + "]");
         } catch (ReflectiveOperationException var6) {
            logger.warn("Error loading Mixin Connector [" + connectorClassName + "]", var6);
         }
      }

      this.connectorClasses.clear();
   }

   void initConnectors() {
      for (IMixinConnector connector : this.connectors) {
         try {
            connector.connect();
         } catch (Exception var4) {
            logger.warn("Error initialising Mixin Connector [" + connector.getClass().getName() + "]", var4);
         }
      }
   }
}
