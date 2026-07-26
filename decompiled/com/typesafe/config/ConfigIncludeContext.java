package com.typesafe.config;

public interface ConfigIncludeContext {
   ConfigParseable relativeTo(String var1);

   ConfigParseOptions parseOptions();
}
