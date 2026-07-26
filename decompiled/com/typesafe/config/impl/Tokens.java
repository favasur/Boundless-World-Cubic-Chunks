package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueType;
import java.util.List;

final class Tokens {
   static final Token START = Token.newWithoutOrigin(TokenType.START, "start of file");
   static final Token END = Token.newWithoutOrigin(TokenType.END, "end of file");
   static final Token COMMA = Token.newWithoutOrigin(TokenType.COMMA, "','");
   static final Token EQUALS = Token.newWithoutOrigin(TokenType.EQUALS, "'='");
   static final Token COLON = Token.newWithoutOrigin(TokenType.COLON, "':'");
   static final Token OPEN_CURLY = Token.newWithoutOrigin(TokenType.OPEN_CURLY, "'{'");
   static final Token CLOSE_CURLY = Token.newWithoutOrigin(TokenType.CLOSE_CURLY, "'}'");
   static final Token OPEN_SQUARE = Token.newWithoutOrigin(TokenType.OPEN_SQUARE, "'['");
   static final Token CLOSE_SQUARE = Token.newWithoutOrigin(TokenType.CLOSE_SQUARE, "']'");
   static final Token PLUS_EQUALS = Token.newWithoutOrigin(TokenType.PLUS_EQUALS, "'+='");

   Tokens() {
   }

   static boolean isValue(Token token) {
      return token instanceof Tokens.Value;
   }

   static AbstractConfigValue getValue(Token token) {
      if (token instanceof Tokens.Value) {
         return ((Tokens.Value)token).value();
      } else {
         throw new ConfigException.BugOrBroken("tried to get value of non-value token " + token);
      }
   }

   static boolean isValueWithType(Token t, ConfigValueType valueType) {
      return isValue(t) && getValue(t).valueType() == valueType;
   }

   static boolean isNewline(Token token) {
      return token instanceof Tokens.Line;
   }

   static boolean isProblem(Token token) {
      return token instanceof Tokens.Problem;
   }

   static String getProblemWhat(Token token) {
      if (token instanceof Tokens.Problem) {
         return ((Tokens.Problem)token).what();
      } else {
         throw new ConfigException.BugOrBroken("tried to get problem what from " + token);
      }
   }

   static String getProblemMessage(Token token) {
      if (token instanceof Tokens.Problem) {
         return ((Tokens.Problem)token).message();
      } else {
         throw new ConfigException.BugOrBroken("tried to get problem message from " + token);
      }
   }

   static boolean getProblemSuggestQuotes(Token token) {
      if (token instanceof Tokens.Problem) {
         return ((Tokens.Problem)token).suggestQuotes();
      } else {
         throw new ConfigException.BugOrBroken("tried to get problem suggestQuotes from " + token);
      }
   }

   static Throwable getProblemCause(Token token) {
      if (token instanceof Tokens.Problem) {
         return ((Tokens.Problem)token).cause();
      } else {
         throw new ConfigException.BugOrBroken("tried to get problem cause from " + token);
      }
   }

   static boolean isComment(Token token) {
      return token instanceof Tokens.Comment;
   }

   static String getCommentText(Token token) {
      if (token instanceof Tokens.Comment) {
         return ((Tokens.Comment)token).text();
      } else {
         throw new ConfigException.BugOrBroken("tried to get comment text from " + token);
      }
   }

   static boolean isUnquotedText(Token token) {
      return token instanceof Tokens.UnquotedText;
   }

   static String getUnquotedText(Token token) {
      if (token instanceof Tokens.UnquotedText) {
         return ((Tokens.UnquotedText)token).value();
      } else {
         throw new ConfigException.BugOrBroken("tried to get unquoted text from " + token);
      }
   }

   static boolean isSubstitution(Token token) {
      return token instanceof Tokens.Substitution;
   }

   static List<Token> getSubstitutionPathExpression(Token token) {
      if (token instanceof Tokens.Substitution) {
         return ((Tokens.Substitution)token).value();
      } else {
         throw new ConfigException.BugOrBroken("tried to get substitution from " + token);
      }
   }

   static boolean getSubstitutionOptional(Token token) {
      if (token instanceof Tokens.Substitution) {
         return ((Tokens.Substitution)token).optional();
      } else {
         throw new ConfigException.BugOrBroken("tried to get substitution optionality from " + token);
      }
   }

   static Token newLine(ConfigOrigin origin) {
      return new Tokens.Line(origin);
   }

   static Token newProblem(ConfigOrigin origin, String what, String message, boolean suggestQuotes, Throwable cause) {
      return new Tokens.Problem(origin, what, message, suggestQuotes, cause);
   }

   static Token newComment(ConfigOrigin origin, String text) {
      return new Tokens.Comment(origin, text);
   }

   static Token newUnquotedText(ConfigOrigin origin, String s) {
      return new Tokens.UnquotedText(origin, s);
   }

   static Token newSubstitution(ConfigOrigin origin, boolean optional, List<Token> expression) {
      return new Tokens.Substitution(origin, optional, expression);
   }

   static Token newValue(AbstractConfigValue value) {
      return new Tokens.Value(value);
   }

   static Token newString(ConfigOrigin origin, String value) {
      return newValue(new ConfigString(origin, value));
   }

   static Token newInt(ConfigOrigin origin, int value, String originalText) {
      return newValue(ConfigNumber.newNumber(origin, (long)value, originalText));
   }

   static Token newDouble(ConfigOrigin origin, double value, String originalText) {
      return newValue(ConfigNumber.newNumber(origin, value, originalText));
   }

   static Token newLong(ConfigOrigin origin, long value, String originalText) {
      return newValue(ConfigNumber.newNumber(origin, value, originalText));
   }

   static Token newNull(ConfigOrigin origin) {
      return newValue(new ConfigNull(origin));
   }

   static Token newBoolean(ConfigOrigin origin, boolean value) {
      return newValue(new ConfigBoolean(origin, value));
   }

   private static class Comment extends Token {
      private final String text;

      Comment(ConfigOrigin origin, String text) {
         super(TokenType.COMMENT, origin);
         this.text = text;
      }

      String text() {
         return this.text;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("'#");
         sb.append(this.text);
         sb.append("' (COMMENT)");
         return sb.toString();
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.Comment;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other) && ((Tokens.Comment)other).text.equals(this.text);
      }

      @Override
      public int hashCode() {
         int h = 41 * (41 + super.hashCode());
         return 41 * (h + this.text.hashCode());
      }
   }

   private static class Line extends Token {
      Line(ConfigOrigin origin) {
         super(TokenType.NEWLINE, origin);
      }

      @Override
      public String toString() {
         return "'\\n'@" + this.lineNumber();
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.Line;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other) && ((Tokens.Line)other).lineNumber() == this.lineNumber();
      }

      @Override
      public int hashCode() {
         return 41 * (41 + super.hashCode()) + this.lineNumber();
      }
   }

   private static class Problem extends Token {
      private final String what;
      private final String message;
      private final boolean suggestQuotes;
      private final Throwable cause;

      Problem(ConfigOrigin origin, String what, String message, boolean suggestQuotes, Throwable cause) {
         super(TokenType.PROBLEM, origin);
         this.what = what;
         this.message = message;
         this.suggestQuotes = suggestQuotes;
         this.cause = cause;
      }

      String what() {
         return this.what;
      }

      String message() {
         return this.message;
      }

      boolean suggestQuotes() {
         return this.suggestQuotes;
      }

      Throwable cause() {
         return this.cause;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append('\'');
         sb.append(this.what);
         sb.append('\'');
         sb.append(" (");
         sb.append(this.message);
         sb.append(")");
         return sb.toString();
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.Problem;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other)
            && ((Tokens.Problem)other).what.equals(this.what)
            && ((Tokens.Problem)other).message.equals(this.message)
            && ((Tokens.Problem)other).suggestQuotes == this.suggestQuotes
            && ConfigImplUtil.equalsHandlingNull(((Tokens.Problem)other).cause, this.cause);
      }

      @Override
      public int hashCode() {
         int h = 41 * (41 + super.hashCode());
         h = 41 * (h + this.what.hashCode());
         h = 41 * (h + this.message.hashCode());
         h = 41 * (h + Boolean.valueOf(this.suggestQuotes).hashCode());
         if (this.cause != null) {
            h = 41 * (h + this.cause.hashCode());
         }

         return h;
      }
   }

   private static class Substitution extends Token {
      private final boolean optional;
      private final List<Token> value;

      Substitution(ConfigOrigin origin, boolean optional, List<Token> expression) {
         super(TokenType.SUBSTITUTION, origin);
         this.optional = optional;
         this.value = expression;
      }

      boolean optional() {
         return this.optional;
      }

      List<Token> value() {
         return this.value;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();

         for (Token t : this.value) {
            sb.append(t.toString());
         }

         return "'${" + sb.toString() + "}'";
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.Substitution;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other) && ((Tokens.Substitution)other).value.equals(this.value);
      }

      @Override
      public int hashCode() {
         return 41 * (41 + super.hashCode()) + this.value.hashCode();
      }
   }

   private static class UnquotedText extends Token {
      private final String value;

      UnquotedText(ConfigOrigin origin, String s) {
         super(TokenType.UNQUOTED_TEXT, origin);
         this.value = s;
      }

      String value() {
         return this.value;
      }

      @Override
      public String toString() {
         return "'" + this.value + "'";
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.UnquotedText;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other) && ((Tokens.UnquotedText)other).value.equals(this.value);
      }

      @Override
      public int hashCode() {
         return 41 * (41 + super.hashCode()) + this.value.hashCode();
      }
   }

   private static class Value extends Token {
      private final AbstractConfigValue value;

      Value(AbstractConfigValue value) {
         super(TokenType.VALUE, value.origin());
         this.value = value;
      }

      AbstractConfigValue value() {
         return this.value;
      }

      @Override
      public String toString() {
         return "'" + this.value().unwrapped() + "' (" + this.value.valueType().name() + ")";
      }

      @Override
      protected boolean canEqual(Object other) {
         return other instanceof Tokens.Value;
      }

      @Override
      public boolean equals(Object other) {
         return super.equals(other) && ((Tokens.Value)other).value.equals(this.value);
      }

      @Override
      public int hashCode() {
         return 41 * (41 + super.hashCode()) + this.value.hashCode();
      }
   }
}
