package org.spongepowered.asm.launch.platform;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class MainAttributes {
   private static final Map<URI, MainAttributes> instances = new HashMap<>();
   protected final Attributes attributes;

   private MainAttributes() {
      this.attributes = new Attributes();
   }

   private MainAttributes(File jar) {
      this.attributes = getAttributes(jar);
   }

   public final String get(String name) {
      return this.attributes != null ? this.attributes.getValue(name) : null;
   }

   private static Attributes getAttributes(File codeSource) {
      if (codeSource == null) {
         return null;
      } else {
         if (codeSource.isFile()) {
            Attributes attributes = getJarAttributes(codeSource);
            if (attributes != null) {
               return attributes;
            }
         }

         if (codeSource.isDirectory()) {
            Attributes attributes = getDirAttributes(codeSource);
            if (attributes != null) {
               return attributes;
            }
         }

         return new Attributes();
      }
   }

   private static Attributes getJarAttributes(File jar) {
      JarFile jarFile = null;

      Attributes var3;
      try {
         jarFile = new JarFile(jar);
         Manifest manifest = jarFile.getManifest();
         if (manifest == null) {
            return null;
         }

         var3 = manifest.getMainAttributes();
      } catch (IOException var14) {
         return null;
      } finally {
         try {
            if (jarFile != null) {
               jarFile.close();
            }
         } catch (IOException var13) {
         }
      }

      return var3;
   }

   private static Attributes getDirAttributes(File dir) {
      File manifestFile = new File(dir, "META-INF/MANIFEST.MF");
      if (manifestFile.isFile()) {
         ByteSource source = Files.asByteSource(manifestFile);
         InputStream inputStream = null;

         Attributes var5;
         try {
            inputStream = source.openBufferedStream();
            Manifest manifest = new Manifest(inputStream);
            var5 = manifest.getMainAttributes();
         } catch (IOException var15) {
            return null;
         } finally {
            try {
               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (IOException var14) {
            }
         }

         return var5;
      } else {
         return null;
      }
   }

   public static MainAttributes of(File jar) {
      return of(jar.toURI());
   }

   public static MainAttributes of(URI uri) {
      MainAttributes attributes = instances.get(uri);
      if (attributes == null) {
         attributes = new MainAttributes(new File(uri));
         instances.put(uri, attributes);
      }

      return attributes;
   }
}
