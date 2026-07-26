package io.github.opencubicchunks.cubicchunks.core.asm;

import com.google.common.eventbus.EventBus;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;

public class CubicChunksCoreContainer extends DummyModContainer {
   public CubicChunksCoreContainer() {
      super(new ModMetadata());
      ModMetadata meta = this.getMetadata();
      meta.modId = "cubicchunkscore";
      meta.name = "Cubic Chunks Coremod";
      meta.version = "1.12.2-0.0.1208.0-SNAPSHOT";
      meta.logoFile = "/assets/cubicchunks/logo.png";
      meta.parent = "cubicchunks";
   }

   public List<ArtifactVersion> getDependencies() {
      return Collections.emptyList();
   }

   public boolean registerBus(EventBus bus, LoadController controller) {
      return true;
   }
}
