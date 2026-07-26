package org.spongepowered.asm.launch.platform.container;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import org.spongepowered.asm.launch.platform.MainAttributes;

public class ContainerHandleURI implements IContainerHandle {
   private final URI uri;
   private final File file;
   private final MainAttributes attributes;

   public ContainerHandleURI(URI uri) {
      this.uri = uri;
      this.file = this.uri != null ? new File(this.uri) : null;
      this.attributes = MainAttributes.of(uri);
   }

   public URI getURI() {
      return this.uri;
   }

   public File getFile() {
      return this.file;
   }

   @Override
   public String getAttribute(String name) {
      return this.attributes.get(name);
   }

   @Override
   public Collection<IContainerHandle> getNestedContainers() {
      return Collections.emptyList();
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof ContainerHandleURI) ? false : this.uri.equals(((ContainerHandleURI)other).uri);
   }

   @Override
   public int hashCode() {
      return this.uri.hashCode();
   }

   @Override
   public String toString() {
      return String.format("ContainerHandleURI(%s)", this.uri);
   }
}
