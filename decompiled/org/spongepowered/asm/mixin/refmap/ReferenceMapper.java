package org.spongepowered.asm.mixin.refmap;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.logging.MessageRouter;

public final class ReferenceMapper implements IReferenceMapper, Serializable {
   private static final long serialVersionUID = 2L;
   public static final String DEFAULT_RESOURCE = "mixin.refmap.json";
   public static final ReferenceMapper DEFAULT_MAPPER = new ReferenceMapper(true, "invalid");
   private final Map<String, Map<String, String>> mappings = Maps.newHashMap();
   private final Map<String, Map<String, Map<String, String>>> data = Maps.newHashMap();
   private final transient boolean readOnly;
   private transient String context = null;
   private transient String resource;

   public ReferenceMapper() {
      this(false, "mixin.refmap.json");
   }

   private ReferenceMapper(boolean readOnly, String resource) {
      this.readOnly = readOnly;
      this.resource = resource;
   }

   @Override
   public boolean isDefault() {
      return this.readOnly;
   }

   private void setResourceName(String resource) {
      if (!this.readOnly) {
         this.resource = resource != null ? resource : "<unknown resource>";
      }
   }

   @Override
   public String getResourceName() {
      return this.resource;
   }

   @Override
   public String getStatus() {
      return this.isDefault() ? "No refMap loaded." : "Using refmap " + this.getResourceName();
   }

   @Override
   public String getContext() {
      return this.context;
   }

   @Override
   public void setContext(String context) {
      this.context = context;
   }

   @Override
   public String remap(String className, String reference) {
      return this.remapWithContext(this.context, className, reference);
   }

   @Override
   public String remapWithContext(String context, String className, String reference) {
      Map<String, Map<String, String>> mappings = this.mappings;
      if (context != null) {
         mappings = this.data.get(context);
         if (mappings == null) {
            mappings = this.mappings;
         }
      }

      return this.remap(mappings, className, reference);
   }

   private String remap(Map<String, Map<String, String>> mappings, String className, String reference) {
      if (className == null) {
         for (Map<String, String> mapping : mappings.values()) {
            if (mapping.containsKey(reference)) {
               return mapping.get(reference);
            }
         }
      }

      Map<String, String> classMappings = mappings.get(className);
      if (classMappings == null) {
         return reference;
      } else {
         String remappedReference = classMappings.get(reference);
         return remappedReference != null ? remappedReference : reference;
      }
   }

   public String addMapping(String context, String className, String reference, String newReference) {
      if (!this.readOnly && reference != null && newReference != null) {
         String conformedReference = reference.replaceAll("\\s", "");
         if (conformedReference.equals(newReference)) {
            return null;
         } else {
            Map<String, Map<String, String>> mappings = this.mappings;
            if (context != null) {
               mappings = this.data.get(context);
               if (mappings == null) {
                  mappings = Maps.newHashMap();
                  this.data.put(context, mappings);
               }
            }

            Map<String, String> classMappings = mappings.get(className);
            if (classMappings == null) {
               classMappings = new HashMap<>();
               mappings.put(className, classMappings);
            }

            return classMappings.put(conformedReference, newReference);
         }
      } else {
         return null;
      }
   }

   public void write(Appendable writer) {
      new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
   }

   public static ReferenceMapper read(String resourcePath) {
      Reader reader = null;

      ReferenceMapper var5;
      try {
         IMixinService service = MixinService.getService();
         InputStream resource = service.getResourceAsStream(resourcePath);
         if (resource == null) {
            return DEFAULT_MAPPER;
         }

         reader = new InputStreamReader(resource);
         ReferenceMapper mapper = readJson(reader);
         mapper.setResourceName(resourcePath);
         var5 = mapper;
      } catch (JsonParseException var10) {
         MessageRouter.getMessager()
            .printMessage(Kind.ERROR, String.format("Invalid REFMAP JSON in %s: %s %s", resourcePath, var10.getClass().getName(), var10.getMessage()));
         return DEFAULT_MAPPER;
      } catch (Exception var11) {
         MessageRouter.getMessager()
            .printMessage(Kind.ERROR, String.format("Failed reading REFMAP JSON from %s: %s %s", resourcePath, var11.getClass().getName(), var11.getMessage()));
         return DEFAULT_MAPPER;
      } finally {
         Closeables.closeQuietly(reader);
      }

      return var5;
   }

   public static ReferenceMapper read(Reader reader, String name) {
      try {
         ReferenceMapper mapper = readJson(reader);
         mapper.setResourceName(name);
         return mapper;
      } catch (Exception var3) {
         return DEFAULT_MAPPER;
      }
   }

   private static ReferenceMapper readJson(Reader reader) {
      return (ReferenceMapper)new Gson().fromJson(reader, ReferenceMapper.class);
   }
}
