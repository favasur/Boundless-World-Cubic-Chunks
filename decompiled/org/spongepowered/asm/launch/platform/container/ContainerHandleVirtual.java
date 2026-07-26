package org.spongepowered.asm.launch.platform.container;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ContainerHandleVirtual implements IContainerHandle {
   private final String name;
   private final Map<String, String> attributes = new HashMap<>();
   private final Set<IContainerHandle> nestedContainers = new LinkedHashSet<>();

   public ContainerHandleVirtual(String name) {
      this.name = name;
   }

   public ContainerHandleVirtual setAttribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
   }

   public ContainerHandleVirtual add(IContainerHandle nested) {
      this.nestedContainers.add(nested);
      return this;
   }

   @Override
   public String getAttribute(String name) {
      return this.attributes.get(name);
   }

   @Override
   public Collection<IContainerHandle> getNestedContainers() {
      return Collections.unmodifiableSet(this.nestedContainers);
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof String && obj.toString().equals(this.name);
   }

   @Override
   public int hashCode() {
      return this.name.hashCode();
   }

   @Override
   public String toString() {
      return String.format("ContainerHandleVirtual(%s:%x)", this.name, this.hashCode());
   }
}
