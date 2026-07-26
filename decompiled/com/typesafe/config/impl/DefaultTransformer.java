package com.typesafe.config.impl;

import com.typesafe.config.ConfigValueType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class DefaultTransformer {
   DefaultTransformer() {
   }

   static AbstractConfigValue transform(AbstractConfigValue value, ConfigValueType requested) {
      if (value.valueType() == ConfigValueType.STRING) {
         String s = (String)value.unwrapped();
         switch (requested) {
            case NUMBER:
               try {
                  Long v = Long.parseLong(s);
                  return new ConfigLong(value.origin(), v, s);
               } catch (NumberFormatException var10) {
                  try {
                     Double vx = Double.parseDouble(s);
                     return new ConfigDouble(value.origin(), vx, s);
                  } catch (NumberFormatException var9) {
                     break;
                  }
               }
            case NULL:
               if (s.equals("null")) {
                  return new ConfigNull(value.origin());
               }
               break;
            case BOOLEAN:
               if (s.equals("true") || s.equals("yes") || s.equals("on")) {
                  return new ConfigBoolean(value.origin(), true);
               }

               if (s.equals("false") || s.equals("no") || s.equals("off")) {
                  return new ConfigBoolean(value.origin(), false);
               }
            case LIST:
            case OBJECT:
            case STRING:
         }
      } else if (requested == ConfigValueType.STRING) {
         switch (value.valueType()) {
            case NUMBER:
            case BOOLEAN:
               return new ConfigString(value.origin(), value.transformToString());
            case NULL:
            case LIST:
            case OBJECT:
            case STRING:
         }
      } else if (requested == ConfigValueType.LIST && value.valueType() == ConfigValueType.OBJECT) {
         AbstractConfigObject o = (AbstractConfigObject)value;
         Map<Integer, AbstractConfigValue> values = new HashMap<>();

         for (String key : o.keySet()) {
            try {
               int i = Integer.parseInt(key, 10);
               if (i >= 0) {
                  values.put(i, o.get(key));
               }
            } catch (NumberFormatException var8) {
            }
         }

         if (!values.isEmpty()) {
            ArrayList<Entry<Integer, AbstractConfigValue>> entryList = new ArrayList<>(values.entrySet());
            Collections.sort(entryList, new Comparator<Entry<Integer, AbstractConfigValue>>() {
               public int compare(Entry<Integer, AbstractConfigValue> a, Entry<Integer, AbstractConfigValue> b) {
                  return Integer.valueOf(a.getKey()).compareTo(b.getKey());
               }
            });
            ArrayList<AbstractConfigValue> list = new ArrayList<>();

            for (Entry<Integer, AbstractConfigValue> entry : entryList) {
               list.add(entry.getValue());
            }

            return new SimpleConfigList(value.origin(), list);
         }
      }

      return value;
   }
}
