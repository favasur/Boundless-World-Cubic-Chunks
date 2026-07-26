package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigSyntax;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

final class Tokenizer {
   Tokenizer() {
   }

   private static String asString(int codepoint) {
      if (codepoint == 10) {
         return "newline";
      } else if (codepoint == 9) {
         return "tab";
      } else if (codepoint == -1) {
         return "end of file";
      } else {
         return Character.isISOControl(codepoint) ? String.format("control character 0x%x", codepoint) : String.format("%c", codepoint);
      }
   }

   static Iterator<Token> tokenize(ConfigOrigin origin, Reader input, ConfigSyntax flavor) {
      return new Tokenizer.TokenIterator(origin, input, flavor != ConfigSyntax.JSON);
   }

   private static class ProblemException extends Exception {
      private static final long serialVersionUID = 1L;
      private final Token problem;

      ProblemException(Token problem) {
         this.problem = problem;
      }

      Token problem() {
         return this.problem;
      }
   }

   private static class TokenIterator implements Iterator<Token> {
      private final SimpleConfigOrigin origin;
      private final Reader input;
      private final LinkedList<Integer> buffer;
      private int lineNumber;
      private ConfigOrigin lineOrigin;
      private final Queue<Token> tokens;
      private final Tokenizer.TokenIterator.WhitespaceSaver whitespaceSaver;
      private final boolean allowComments;
      static final String firstNumberChars = "0123456789-";
      static final String numberChars = "0123456789eE+-.";
      static final String notInUnquotedText = "$\"{}[]:=,+#`^?!@*&\\";

      TokenIterator(ConfigOrigin origin, Reader input, boolean allowComments) {
         this.origin = (SimpleConfigOrigin)origin;
         this.input = input;
         this.allowComments = allowComments;
         this.buffer = new LinkedList<>();
         this.lineNumber = 1;
         this.lineOrigin = this.origin.setLineNumber(this.lineNumber);
         this.tokens = new LinkedList<>();
         this.tokens.add(Tokens.START);
         this.whitespaceSaver = new Tokenizer.TokenIterator.WhitespaceSaver();
      }

      private int nextCharRaw() {
         if (this.buffer.isEmpty()) {
            try {
               return this.input.read();
            } catch (IOException var2) {
               throw new ConfigException.IO(this.origin, "read error: " + var2.getMessage(), var2);
            }
         } else {
            return this.buffer.pop();
         }
      }

      private void putBack(int c) {
         if (this.buffer.size() > 2) {
            throw new ConfigException.BugOrBroken("bug: putBack() three times, undesirable look-ahead");
         } else {
            this.buffer.push(c);
         }
      }

      static boolean isWhitespace(int c) {
         return ConfigImplUtil.isWhitespace(c);
      }

      static boolean isWhitespaceNotNewline(int c) {
         return c != 10 && ConfigImplUtil.isWhitespace(c);
      }

      private boolean startOfComment(int c) {
         if (c == -1) {
            return false;
         } else if (this.allowComments) {
            if (c == 35) {
               return true;
            } else if (c == 47) {
               int maybeSecondSlash = this.nextCharRaw();
               this.putBack(maybeSecondSlash);
               return maybeSecondSlash == 47;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      private int nextCharAfterWhitespace(Tokenizer.TokenIterator.WhitespaceSaver saver) {
         while (true) {
            int c = this.nextCharRaw();
            if (c == -1) {
               return -1;
            }

            if (!isWhitespaceNotNewline(c)) {
               return c;
            }

            saver.add(c);
         }
      }

      private Tokenizer.ProblemException problem(String message) {
         return this.problem("", message, null);
      }

      private Tokenizer.ProblemException problem(String what, String message) {
         return this.problem(what, message, null);
      }

      private Tokenizer.ProblemException problem(String what, String message, boolean suggestQuotes) {
         return this.problem(what, message, suggestQuotes, null);
      }

      private Tokenizer.ProblemException problem(String what, String message, Throwable cause) {
         return problem(this.lineOrigin, what, message, cause);
      }

      private Tokenizer.ProblemException problem(String what, String message, boolean suggestQuotes, Throwable cause) {
         return problem(this.lineOrigin, what, message, suggestQuotes, cause);
      }

      private static Tokenizer.ProblemException problem(ConfigOrigin origin, String what, String message, Throwable cause) {
         return problem(origin, what, message, false, cause);
      }

      private static Tokenizer.ProblemException problem(ConfigOrigin origin, String what, String message, boolean suggestQuotes, Throwable cause) {
         if (what != null && message != null) {
            return new Tokenizer.ProblemException(Tokens.newProblem(origin, what, message, suggestQuotes, cause));
         } else {
            throw new ConfigException.BugOrBroken("internal error, creating bad ProblemException");
         }
      }

      private static Tokenizer.ProblemException problem(ConfigOrigin origin, String message) {
         return problem(origin, "", message, null);
      }

      private static ConfigOrigin lineOrigin(ConfigOrigin baseOrigin, int lineNumber) {
         return ((SimpleConfigOrigin)baseOrigin).setLineNumber(lineNumber);
      }

      private Token pullComment(int firstChar) {
         if (firstChar == 47) {
            int discard = this.nextCharRaw();
            if (discard != 47) {
               throw new ConfigException.BugOrBroken("called pullComment but // not seen");
            }
         }

         StringBuilder sb = new StringBuilder();

         while (true) {
            int c = this.nextCharRaw();
            if (c == -1 || c == 10) {
               this.putBack(c);
               return Tokens.newComment(this.lineOrigin, sb.toString());
            }

            sb.appendCodePoint(c);
         }
      }

      private Token pullUnquotedText() {
         ConfigOrigin origin = this.lineOrigin;
         StringBuilder sb = new StringBuilder();

         int c;
         for (c = this.nextCharRaw(); c != -1 && "$\"{}[]:=,+#`^?!@*&\\".indexOf(c) < 0 && !isWhitespace(c) && !this.startOfComment(c); c = this.nextCharRaw()) {
            sb.appendCodePoint(c);
            if (sb.length() == 4) {
               String s = sb.toString();
               if (s.equals("true")) {
                  return Tokens.newBoolean(origin, true);
               }

               if (s.equals("null")) {
                  return Tokens.newNull(origin);
               }
            } else if (sb.length() == 5) {
               String sx = sb.toString();
               if (sx.equals("false")) {
                  return Tokens.newBoolean(origin, false);
               }
            }
         }

         this.putBack(c);
         String sx = sb.toString();
         return Tokens.newUnquotedText(origin, sx);
      }

      private Token pullNumber(int firstChar) throws Tokenizer.ProblemException {
         StringBuilder sb = new StringBuilder();
         sb.appendCodePoint(firstChar);
         boolean containedDecimalOrE = false;

         int c;
         for (c = this.nextCharRaw(); c != -1 && "0123456789eE+-.".indexOf(c) >= 0; c = this.nextCharRaw()) {
            if (c == 46 || c == 101 || c == 69) {
               containedDecimalOrE = true;
            }

            sb.appendCodePoint(c);
         }

         this.putBack(c);
         String s = sb.toString();

         try {
            return containedDecimalOrE ? Tokens.newDouble(this.lineOrigin, Double.parseDouble(s), s) : Tokens.newLong(this.lineOrigin, Long.parseLong(s), s);
         } catch (NumberFormatException var11) {
            for (char u : s.toCharArray()) {
               if ("$\"{}[]:=,+#`^?!@*&\\".indexOf(u) >= 0) {
                  throw this.problem(Tokenizer.asString(u), "Reserved character '" + Tokenizer.asString(u) + "' is not allowed outside quotes", true);
               }
            }

            return Tokens.newUnquotedText(this.lineOrigin, s);
         }
      }

      private void pullEscapeSequence(StringBuilder sb) throws Tokenizer.ProblemException {
         int escaped = this.nextCharRaw();
         if (escaped == -1) {
            throw this.problem("End of input but backslash in string had nothing after it");
         } else {
            switch (escaped) {
               case 34:
                  sb.append('"');
                  break;
               case 47:
                  sb.append('/');
                  break;
               case 92:
                  sb.append('\\');
                  break;
               case 98:
                  sb.append('\b');
                  break;
               case 102:
                  sb.append('\f');
                  break;
               case 110:
                  sb.append('\n');
                  break;
               case 114:
                  sb.append('\r');
                  break;
               case 116:
                  sb.append('\t');
                  break;
               case 117:
                  char[] a = new char[4];

                  for (int i = 0; i < 4; i++) {
                     int c = this.nextCharRaw();
                     if (c == -1) {
                        throw this.problem("End of input but expecting 4 hex digits for \\uXXXX escape");
                     }

                     a[i] = (char)c;
                  }

                  String digits = new String(a);

                  try {
                     sb.appendCodePoint(Integer.parseInt(digits, 16));
                     break;
                  } catch (NumberFormatException var6) {
                     throw this.problem(digits, String.format("Malformed hex digits after \\u escape in string: '%s'", digits), var6);
                  }
               default:
                  throw this.problem(
                     Tokenizer.asString(escaped),
                     String.format(
                        "backslash followed by '%s', this is not a valid escape sequence (quoted strings use JSON escaping, so use double-backslash \\\\ for literal backslash)",
                        Tokenizer.asString(escaped)
                     )
                  );
            }
         }
      }

      private void appendTripleQuotedString(StringBuilder sb) throws Tokenizer.ProblemException {
         int consecutiveQuotes = 0;

         while (true) {
            int c = this.nextCharRaw();
            if (c == 34) {
               consecutiveQuotes++;
            } else {
               if (consecutiveQuotes >= 3) {
                  sb.setLength(sb.length() - 3);
                  this.putBack(c);
                  return;
               }

               consecutiveQuotes = 0;
               if (c == -1) {
                  throw this.problem("End of input but triple-quoted string was still open");
               }

               if (c == 10) {
                  this.lineNumber++;
                  this.lineOrigin = this.origin.setLineNumber(this.lineNumber);
               }
            }

            sb.appendCodePoint(c);
         }
      }

      private Token pullQuotedString() throws Tokenizer.ProblemException {
         StringBuilder sb = new StringBuilder();
         int c = 0;

         do {
            c = this.nextCharRaw();
            if (c == -1) {
               throw this.problem("End of input but string quote was still open");
            }

            if (c == 92) {
               this.pullEscapeSequence(sb);
            } else if (c != 34) {
               if (Character.isISOControl(c)) {
                  throw this.problem(
                     Tokenizer.asString(c), "JSON does not allow unescaped " + Tokenizer.asString(c) + " in quoted strings, use a backslash escape"
                  );
               }

               sb.appendCodePoint(c);
            }
         } while (c != 34);

         if (sb.length() == 0) {
            int third = this.nextCharRaw();
            if (third == 34) {
               this.appendTripleQuotedString(sb);
            } else {
               this.putBack(third);
            }
         }

         return Tokens.newString(this.lineOrigin, sb.toString());
      }

      private Token pullPlusEquals() throws Tokenizer.ProblemException {
         int c = this.nextCharRaw();
         if (c != 61) {
            throw this.problem(Tokenizer.asString(c), "'+' not followed by =, '" + Tokenizer.asString(c) + "' not allowed after '+'", true);
         } else {
            return Tokens.PLUS_EQUALS;
         }
      }

      private Token pullSubstitution() throws Tokenizer.ProblemException {
         ConfigOrigin origin = this.lineOrigin;
         int c = this.nextCharRaw();
         if (c != 123) {
            throw this.problem(Tokenizer.asString(c), "'$' not followed by {, '" + Tokenizer.asString(c) + "' not allowed after '$'", true);
         } else {
            boolean optional = false;
            c = this.nextCharRaw();
            if (c == 63) {
               optional = true;
            } else {
               this.putBack(c);
            }

            Tokenizer.TokenIterator.WhitespaceSaver saver = new Tokenizer.TokenIterator.WhitespaceSaver();
            List<Token> expression = new ArrayList<>();

            while (true) {
               Token t = this.pullNextToken(saver);
               if (t == Tokens.CLOSE_CURLY) {
                  return Tokens.newSubstitution(origin, optional, expression);
               }

               if (t == Tokens.END) {
                  throw problem(origin, "Substitution ${ was not closed with a }");
               }

               Token whitespace = saver.check(t, origin, this.lineNumber);
               if (whitespace != null) {
                  expression.add(whitespace);
               }

               expression.add(t);
            }
         }
      }

      private Token pullNextToken(Tokenizer.TokenIterator.WhitespaceSaver saver) throws Tokenizer.ProblemException {
         int c = this.nextCharAfterWhitespace(saver);
         if (c == -1) {
            return Tokens.END;
         } else if (c == 10) {
            Token line = Tokens.newLine(this.lineOrigin);
            this.lineNumber++;
            this.lineOrigin = this.origin.setLineNumber(this.lineNumber);
            return line;
         } else {
            Token t;
            if (this.startOfComment(c)) {
               t = this.pullComment(c);
            } else {
               switch (c) {
                  case 34:
                     t = this.pullQuotedString();
                     break;
                  case 36:
                     t = this.pullSubstitution();
                     break;
                  case 43:
                     t = this.pullPlusEquals();
                     break;
                  case 44:
                     t = Tokens.COMMA;
                     break;
                  case 58:
                     t = Tokens.COLON;
                     break;
                  case 61:
                     t = Tokens.EQUALS;
                     break;
                  case 91:
                     t = Tokens.OPEN_SQUARE;
                     break;
                  case 93:
                     t = Tokens.CLOSE_SQUARE;
                     break;
                  case 123:
                     t = Tokens.OPEN_CURLY;
                     break;
                  case 125:
                     t = Tokens.CLOSE_CURLY;
                     break;
                  default:
                     t = null;
               }

               if (t == null) {
                  if ("0123456789-".indexOf(c) >= 0) {
                     t = this.pullNumber(c);
                  } else {
                     if ("$\"{}[]:=,+#`^?!@*&\\".indexOf(c) >= 0) {
                        throw this.problem(Tokenizer.asString(c), "Reserved character '" + Tokenizer.asString(c) + "' is not allowed outside quotes", true);
                     }

                     this.putBack(c);
                     t = this.pullUnquotedText();
                  }
               }
            }

            if (t == null) {
               throw new ConfigException.BugOrBroken("bug: failed to generate next token");
            } else {
               return t;
            }
         }
      }

      private static boolean isSimpleValue(Token t) {
         return Tokens.isSubstitution(t) || Tokens.isUnquotedText(t) || Tokens.isValue(t);
      }

      private void queueNextToken() throws Tokenizer.ProblemException {
         Token t = this.pullNextToken(this.whitespaceSaver);
         Token whitespace = this.whitespaceSaver.check(t, this.origin, this.lineNumber);
         if (whitespace != null) {
            this.tokens.add(whitespace);
         }

         this.tokens.add(t);
      }

      @Override
      public boolean hasNext() {
         return !this.tokens.isEmpty();
      }

      public Token next() {
         Token t = this.tokens.remove();
         if (this.tokens.isEmpty() && t != Tokens.END) {
            try {
               this.queueNextToken();
            } catch (Tokenizer.ProblemException var3) {
               this.tokens.add(var3.problem());
            }

            if (this.tokens.isEmpty()) {
               throw new ConfigException.BugOrBroken("bug: tokens queue should not be empty here");
            }
         }

         return t;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException("Does not make sense to remove items from token stream");
      }

      private static class WhitespaceSaver {
         private StringBuilder whitespace = new StringBuilder();
         private boolean lastTokenWasSimpleValue = false;

         WhitespaceSaver() {
         }

         void add(int c) {
            if (this.lastTokenWasSimpleValue) {
               this.whitespace.appendCodePoint(c);
            }
         }

         Token check(Token t, ConfigOrigin baseOrigin, int lineNumber) {
            if (Tokenizer.TokenIterator.isSimpleValue(t)) {
               return this.nextIsASimpleValue(baseOrigin, lineNumber);
            } else {
               this.nextIsNotASimpleValue();
               return null;
            }
         }

         private void nextIsNotASimpleValue() {
            this.lastTokenWasSimpleValue = false;
            this.whitespace.setLength(0);
         }

         private Token nextIsASimpleValue(ConfigOrigin baseOrigin, int lineNumber) {
            if (this.lastTokenWasSimpleValue) {
               if (this.whitespace.length() > 0) {
                  Token t = Tokens.newUnquotedText(Tokenizer.TokenIterator.lineOrigin(baseOrigin, lineNumber), this.whitespace.toString());
                  this.whitespace.setLength(0);
                  return t;
               } else {
                  return null;
               }
            } else {
               this.lastTokenWasSimpleValue = true;
               this.whitespace.setLength(0);
               return null;
            }
         }
      }
   }
}
