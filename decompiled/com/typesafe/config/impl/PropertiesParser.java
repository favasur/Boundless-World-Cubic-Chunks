package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

final class PropertiesParser {
   PropertiesParser() {
   }

   static AbstractConfigObject parse(Reader reader, ConfigOrigin origin) throws IOException {
      Properties props = new Properties();
      props.load(reader);
      return fromProperties(origin, props);
   }

   static String lastElement(String path) {
      int i = path.lastIndexOf(46);
      return i < 0 ? path : path.substring(i + 1);
   }

   static String exceptLastElement(String path) {
      int i = path.lastIndexOf(46);
      return i < 0 ? null : path.substring(0, i);
   }

   static Path pathFromPropertyKey(String key) {
      String last = lastElement(key);
      String exceptLast = exceptLastElement(key);
      Path path = new Path(last, null);

      while (exceptLast != null) {
         last = lastElement(exceptLast);
         exceptLast = exceptLastElement(exceptLast);
         path = new Path(last, path);
      }

      return path;
   }

   static AbstractConfigObject fromProperties(ConfigOrigin origin, Properties props) {
      Map<Path, Object> pathMap = new HashMap<>();

      for (Entry<Object, Object> entry : props.entrySet()) {
         Object key = entry.getKey();
         if (key instanceof String) {
            Path path = pathFromPropertyKey((String)key);
            pathMap.put(path, entry.getValue());
         }
      }

      return fromPathMap(origin, pathMap, true);
   }

   static AbstractConfigObject fromPathMap(ConfigOrigin origin, Map<?, ?> pathExpressionMap) {
      Map<Path, Object> pathMap = new HashMap<>();

      for (Entry<?, ?> entry : pathExpressionMap.entrySet()) {
         Object keyObj = entry.getKey();
         if (!(keyObj instanceof String)) {
            throw new ConfigException.BugOrBroken("Map has a non-string as a key, expecting a path expression as a String");
         }

         Path path = Path.newPath((String)keyObj);
         pathMap.put(path, entry.getValue());
      }

      return fromPathMap(origin, pathMap, false);
   }

   private static AbstractConfigObject fromPathMap(ConfigOrigin origin, Map<Path, Object> pathMap, boolean convertedFromProperties) {
      Set<Path> scopePaths = new HashSet<>();
      Set<Path> valuePaths = new HashSet<>();

      for (Path path : pathMap.keySet()) {
         valuePaths.add(path);

         for (Path next = path.parent(); next != null; next = next.parent()) {
            scopePaths.add(next);
         }
      }

      if (convertedFromProperties) {
         valuePaths.removeAll(scopePaths);
      } else {
         for (Path path : valuePaths) {
            if (scopePaths.contains(path)) {
               throw new ConfigException.BugOrBroken(
                  "In the map, path '"
                     + path.render()
                     + "' occurs as both the parent object of a value and as a value. "
                     + "Because Map has no defined ordering, this is a broken situation."
               );
            }
         }
      }

      Map<String, AbstractConfigValue> root = new HashMap<>();
      Map<Path, Map<String, AbstractConfigValue>> scopes = new HashMap<>();

      for (Path pathx : scopePaths) {
         Map<String, AbstractConfigValue> scope = new HashMap<>();
         scopes.put(pathx, scope);
      }

      for (Path pathx : valuePaths) {
         Path parentPath = pathx.parent();
         Map<String, AbstractConfigValue> parent = parentPath != null ? scopes.get(parentPath) : root;
         String last = pathx.last();
         Object rawValue = pathMap.get(pathx);
         AbstractConfigValue value;
         if (convertedFromProperties) {
            value = new ConfigString(origin, (String)rawValue);
         } else {
            value = ConfigImpl.fromAnyRef(pathMap.get(pathx), origin, FromMapMode.KEYS_ARE_PATHS);
         }

         parent.put(last, value);
      }

      List<Path> sortedScopePaths = new ArrayList<>();
      sortedScopePaths.addAll(scopePaths);
      Collections.sort(sortedScopePaths, new Comparator<Path>() {
         public int compare(Path a, Path b) {
            return b.length() - a.length();
         }
      });

      for (Path scopePath : sortedScopePaths) {
         Map<String, AbstractConfigValue> scope = scopes.get(scopePath);
         Path parentPath = scopePath.parent();
         Map<String, AbstractConfigValue> parent = parentPath != null ? scopes.get(parentPath) : root;
         AbstractConfigObject o = new SimpleConfigObject(origin, scope, ResolveStatus.RESOLVED, false);
         parent.put(scopePath.last(), o);
      }

      return new SimpleConfigObject(origin, root, ResolveStatus.RESOLVED, false);
   }
}
