package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;

class Token {
   private final TokenType tokenType;
   private final String debugString;
   private final ConfigOrigin origin;

   Token(TokenType tokenType, ConfigOrigin origin) {
      this(tokenType, origin, null);
   }

   Token(TokenType tokenType, ConfigOrigin origin, String debugString) {
      this.tokenType = tokenType;
      this.origin = origin;
      this.debugString = debugString;
   }

   static Token newWithoutOrigin(TokenType tokenType, String debugString) {
      return new Token(tokenType, null, debugString);
   }

   final TokenType tokenType() {
      return this.tokenType;
   }

   final ConfigOrigin origin() {
      if (this.origin == null) {
         throw new ConfigException.BugOrBroken("tried to get origin from token that doesn't have one: " + this);
      } else {
         return this.origin;
      }
   }

   final int lineNumber() {
      return this.origin != null ? this.origin.lineNumber() : -1;
   }

   @Override
   public String toString() {
      return this.debugString != null ? this.debugString : this.tokenType.name();
   }

   protected boolean canEqual(Object other) {
      return other instanceof Token;
   }

   @Override
   public boolean equals(Object other) {
      return !(other instanceof Token) ? false : this.canEqual(other) && this.tokenType == ((Token)other).tokenType;
   }

   @Override
   public int hashCode() {
      return this.tokenType.hashCode();
   }
}
