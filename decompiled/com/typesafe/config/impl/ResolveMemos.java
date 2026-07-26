package com.typesafe.config.impl;

import java.util.HashMap;
import java.util.Map;

final class ResolveMemos {
   private final Map<MemoKey, AbstractConfigValue> memos = new HashMap<>();

   ResolveMemos() {
   }

   AbstractConfigValue get(MemoKey key) {
      return this.memos.get(key);
   }

   void put(MemoKey key, AbstractConfigValue value) {
      this.memos.put(key, value);
   }
}
