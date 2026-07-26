package com.typesafe.config.impl;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueType;
import java.io.ObjectStreamException;
import java.io.Serializable;

final class ConfigLong extends ConfigNumber implements Serializable {
   private static final long serialVersionUID = 2L;
   private final long value;

   ConfigLong(ConfigOrigin origin, long value, String originalText) {
      super(origin, originalText);
      this.value = value;
   }

   @Override
   public ConfigValueType valueType() {
      return ConfigValueType.NUMBER;
   }

   public Long unwrapped() {
      return this.value;
   }

   @Override
   String transformToString() {
      String s = super.transformToString();
      return s == null ? Long.toString(this.value) : s;
   }

   @Override
   protected long longValue() {
      return this.value;
   }

   @Override
   protected double doubleValue() {
      return (double)this.value;
   }

   protected ConfigLong newCopy(ConfigOrigin origin) {
      return new ConfigLong(origin, this.value, this.originalText);
   }

   private Object writeReplace() throws ObjectStreamException {
      return new SerializedConfigValue(this);
   }
}
