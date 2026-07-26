package org.spongepowered.tools.obfuscation.mcp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Collection;
import java.util.Set;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

public class ObfuscationServiceMCP implements IObfuscationService {
   public static final String SEARGE = "searge";
   public static final String NOTCH = "notch";
   public static final String REOBF_SRG_FILE = "reobfSrgFile";
   public static final String REOBF_EXTRA_SRG_FILES = "reobfSrgFiles";
   public static final String REOBF_NOTCH_FILE = "reobfNotchSrgFile";
   public static final String REOBF_EXTRA_NOTCH_FILES = "reobfNotchSrgFiles";
   public static final String OUT_SRG_SRG_FILE = "outSrgFile";
   public static final String OUT_NOTCH_SRG_FILE = "outNotchSrgFile";

   public ObfuscationServiceMCP() {
   }

   @Override
   public Set<String> getSupportedOptions() {
      return ImmutableSet.of("reobfSrgFile", "reobfSrgFiles", "reobfNotchSrgFile", "reobfNotchSrgFiles", "outSrgFile", "outNotchSrgFile", new String[0]);
   }

   @Override
   public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor ap) {
      Builder<ObfuscationTypeDescriptor> list = ImmutableList.builder();
      if (!ap.getOptions("mappingTypes").contains("tsrg")) {
         list.add(new ObfuscationTypeDescriptor("searge", "reobfSrgFile", "reobfSrgFiles", "outSrgFile", ObfuscationEnvironmentMCP.class));
      }

      list.add(new ObfuscationTypeDescriptor("notch", "reobfNotchSrgFile", "reobfNotchSrgFiles", "outNotchSrgFile", ObfuscationEnvironmentMCP.class));
      return list.build();
   }
}
