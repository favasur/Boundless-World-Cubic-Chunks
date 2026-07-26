package org.spongepowered.asm.launch.platform.container;

import java.util.Collection;

public interface IContainerHandle {
   String getAttribute(String var1);

   Collection<IContainerHandle> getNestedContainers();
}
