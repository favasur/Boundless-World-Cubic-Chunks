package com.typesafe.config.impl;

import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigParseable;

class SimpleIncludeContext implements ConfigIncludeContext {
   private final Parseable parseable;

   SimpleIncludeContext(Parseable parseable) {
      this.parseable = parseable;
   }

   SimpleIncludeContext withParseable(Parseable parseable) {
      return parseable == this.parseable ? this : new SimpleIncludeContext(parseable);
   }

   @Override
   public ConfigParseable relativeTo(String filename) {
      return this.parseable != null ? this.parseable.relativeTo(filename) : null;
   }

   @Override
   public ConfigParseOptions parseOptions() {
      return SimpleIncluder.clearForInclude(this.parseable.options());
   }
}
