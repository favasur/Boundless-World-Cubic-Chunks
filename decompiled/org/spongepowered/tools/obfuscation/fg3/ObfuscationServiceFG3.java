package org.spongepowered.tools.obfuscation.fg3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Collection;
import java.util.Set;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.service.IObfuscationService;
import org.spongepowered.tools.obfuscation.service.ObfuscationTypeDescriptor;

public class ObfuscationServiceFG3 implements IObfuscationService {
   public static final String SEARGE = "searge";
   public static final String REOBF_TSRG_FILE = "reobfTsrgFile";
   public static final String REOBF_EXTRA_TSRG_FILES = "reobfTsrgFiles";
   public static final String OUT_TSRG_SRG_FILE = "outTsrgFile";
   public static final String TSRG_OUTPUT_BEHAVIOUR = "mergeBehaviour";

   public ObfuscationServiceFG3() {
   }

   @Override
   public Set<String> getSupportedOptions() {
      return ImmutableSet.of("reobfTsrgFile", "outTsrgFile", "mergeBehaviour");
   }

   @Override
   public Collection<ObfuscationTypeDescriptor> getObfuscationTypes(IMixinAnnotationProcessor ap) {
      Builder<ObfuscationTypeDescriptor> list = ImmutableList.builder();
      if (ap.getOptions("mappingTypes").contains("tsrg")) {
         list.add(new ObfuscationTypeDescriptor("searge", "reobfTsrgFile", "reobfTsrgFiles", "outTsrgFile", ObfuscationEnvironmentFG3.class));
      }

      return list.build();
   }
}
