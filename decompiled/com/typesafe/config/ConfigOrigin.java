package com.typesafe.config;

import java.net.URL;
import java.util.List;

public interface ConfigOrigin {
   String description();

   String filename();

   URL url();

   String resource();

   int lineNumber();

   List<String> comments();
}
