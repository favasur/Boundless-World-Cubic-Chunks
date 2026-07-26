package org.spongepowered.tools.obfuscation.mapping.fg3;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mapping.mcp.MappingWriterSrg;

public class MappingWriterTSrg extends MappingWriterSrg {
   private final MappingProviderTSrg provider;
   private final boolean mergeExisting;

   public MappingWriterTSrg(Messager messager, Filer filer, MappingProviderTSrg provider, boolean mergeExisting) {
      super(messager, filer);
      this.provider = provider;
      this.mergeExisting = mergeExisting;
   }

   @Override
   protected PrintWriter openFileWriter(String output, ObfuscationType type) throws IOException {
      return this.openFileWriter(output, type + " composite mappings");
   }

   @Override
   protected void writeHeader(PrintWriter writer) {
      if (this.mergeExisting) {
         for (String line : this.provider.getInputMappings()) {
            writer.println(line);
         }
      }
   }

   @Override
   protected String formatFieldMapping(IMappingConsumer.MappingSet.Pair<MappingField> field) {
      return String.format("%s %s %s", field.from.getOwner(), field.from.getSimpleName(), field.to.getSimpleName());
   }

   @Override
   protected String formatMethodMapping(IMappingConsumer.MappingSet.Pair<MappingMethod> method) {
      return String.format("%s %s %s %s", method.from.getOwner(), method.from.getSimpleName(), method.from.getDesc(), method.to.getSimpleName());
   }
}
