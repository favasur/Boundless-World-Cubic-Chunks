package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import java.util.Stack;

final class PathBuilder {
   private final Stack<String> keys = new Stack<>();
   private Path result;

   PathBuilder() {
   }

   private void checkCanAppend() {
      if (this.result != null) {
         throw new ConfigException.BugOrBroken("Adding to PathBuilder after getting result");
      }
   }

   void appendKey(String key) {
      this.checkCanAppend();
      this.keys.push(key);
   }

   void appendPath(Path path) {
      this.checkCanAppend();
      String first = path.first();
      Path remainder = path.remainder();

      while (true) {
         this.keys.push(first);
         if (remainder == null) {
            return;
         }

         first = remainder.first();
         remainder = remainder.remainder();
      }
   }

   Path result() {
      if (this.result == null) {
         Path remainder = null;

         while (!this.keys.isEmpty()) {
            String key = this.keys.pop();
            remainder = new Path(key, remainder);
         }

         this.result = remainder;
      }

      return this.result;
   }
}
