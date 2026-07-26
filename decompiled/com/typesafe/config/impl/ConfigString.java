package com.typesafe.config.impl;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueType;
import java.io.ObjectStreamException;
import java.io.Serializable;

final class ConfigString extends AbstractConfigValue implements Serializable {
   private static final long serialVersionUID = 2L;
   private final String value;

   ConfigString(ConfigOrigin origin, String value) {
      super(origin);
      this.value = value;
   }

   @Override
   public ConfigValueType valueType() {
      return ConfigValueType.STRING;
   }

   public String unwrapped() {
      return this.value;
   }

   @Override
   String transformToString() {
      return this.value;
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      String rendered;
      if (options.getJson()) {
         rendered = ConfigImplUtil.renderJsonString(this.value);
      } else {
         rendered = ConfigImplUtil.renderStringUnquotedIfPossible(this.value);
      }

      sb.append(rendered);
   }

   protected ConfigString newCopy(ConfigOrigin origin) {
      return new ConfigString(origin, this.value);
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SerializedConfigValue(this);
   }
}
