package com.typesafe.config.impl;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValueType;
import java.io.ObjectStreamException;
import java.io.Serializable;

final class ConfigNull extends AbstractConfigValue implements Serializable {
   private static final long serialVersionUID = 2L;

   ConfigNull(ConfigOrigin origin) {
      super(origin);
   }

   @Override
   public ConfigValueType valueType() {
      return ConfigValueType.NULL;
   }

   @Override
   public Object unwrapped() {
      return null;
   }

   @Override
   String transformToString() {
      return "null";
   }

   @Override
   protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
      sb.append("null");
   }

   protected ConfigNull newCopy(ConfigOrigin origin) {
      return new ConfigNull(origin);
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SerializedConfigValue(this);
   }
}
