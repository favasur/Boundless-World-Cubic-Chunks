package com.typesafe.config;

import java.util.List;

public interface ConfigList extends List<ConfigValue>, ConfigValue {
   List<Object> unwrapped();
}
