package com.typesafe.config;

public interface ConfigParseable {
   ConfigObject parse(ConfigParseOptions var1);

   ConfigOrigin origin();

   ConfigParseOptions options();
}
