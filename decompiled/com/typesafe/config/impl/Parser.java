package com.typesafe.config.impl;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigValueType;
import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

final class Parser {
   static ConfigOrigin apiOrigin = SimpleConfigOrigin.newSimple("path parameter");

   Parser() {
   }

   static AbstractConfigValue parse(Iterator<Token> tokens, ConfigOrigin origin, ConfigParseOptions options, ConfigIncludeContext includeContext) {
      Parser.ParseContext context = new Parser.ParseContext(options.getSyntax(), origin, tokens, SimpleIncluder.makeFull(options.getIncluder()), includeContext);
      return context.parse();
   }

   private static void addPathText(List<Parser.Element> buf, boolean wasQuoted, String newText) {
      int i = wasQuoted ? -1 : newText.indexOf(46);
      Parser.Element current = buf.get(buf.size() - 1);
      if (i < 0) {
         current.sb.append(newText);
         if (wasQuoted && current.sb.length() == 0) {
            current.canBeEmpty = true;
         }
      } else {
         current.sb.append(newText.substring(0, i));
         buf.add(new Parser.Element("", false));
         addPathText(buf, false, newText.substring(i + 1));
      }
   }

   private static Path parsePathExpression(Iterator<Token> expression, ConfigOrigin origin) {
      return parsePathExpression(expression, origin, null);
   }

   private static Path parsePathExpression(Iterator<Token> expression, ConfigOrigin origin, String originalText) {
      List<Parser.Element> buf = new ArrayList<>();
      buf.add(new Parser.Element("", false));
      if (!expression.hasNext()) {
         throw new ConfigException.BadPath(origin, originalText, "Expecting a field name or path here, but got nothing");
      } else {
         while (expression.hasNext()) {
            Token t = expression.next();
            if (Tokens.isValueWithType(t, ConfigValueType.STRING)) {
               AbstractConfigValue v = Tokens.getValue(t);
               String s = v.transformToString();
               addPathText(buf, true, s);
            } else if (t != Tokens.END) {
               String text;
               if (Tokens.isValue(t)) {
                  AbstractConfigValue v = Tokens.getValue(t);
                  text = v.transformToString();
               } else {
                  if (!Tokens.isUnquotedText(t)) {
                     throw new ConfigException.BadPath(
                        origin, originalText, "Token not allowed in path expression: " + t + " (you can double-quote this token if you really want it here)"
                     );
                  }

                  text = Tokens.getUnquotedText(t);
               }

               addPathText(buf, false, text);
            }
         }

         PathBuilder pb = new PathBuilder();

         for (Parser.Element e : buf) {
            if (e.sb.length() == 0 && !e.canBeEmpty) {
               throw new ConfigException.BadPath(
                  origin, originalText, "path has a leading, trailing, or two adjacent period '.' (use quoted \"\" empty string if you want an empty element)"
               );
            }

            pb.appendKey(e.sb.toString());
         }

         return pb.result();
      }
   }

   static Path parsePath(String path) {
      Path speculated = speculativeFastParsePath(path);
      if (speculated != null) {
         return speculated;
      } else {
         StringReader reader = new StringReader(path);

         Path var4;
         try {
            Iterator<Token> tokens = Tokenizer.tokenize(apiOrigin, reader, ConfigSyntax.CONF);
            tokens.next();
            var4 = parsePathExpression(tokens, apiOrigin, path);
         } finally {
            reader.close();
         }

         return var4;
      }
   }

   private static boolean hasUnsafeChars(String s) {
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (!Character.isLetter(c) && c != '.') {
            return true;
         }
      }

      return false;
   }

   private static void appendPathString(PathBuilder pb, String s) {
      int splitAt = s.indexOf(46);
      if (splitAt < 0) {
         pb.appendKey(s);
      } else {
         pb.appendKey(s.substring(0, splitAt));
         appendPathString(pb, s.substring(splitAt + 1));
      }
   }

   private static Path speculativeFastParsePath(String path) {
      String s = ConfigImplUtil.unicodeTrim(path);
      if (s.isEmpty()) {
         return null;
      } else if (hasUnsafeChars(s)) {
         return null;
      } else if (!s.startsWith(".") && !s.endsWith(".") && !s.contains("..")) {
         PathBuilder pb = new PathBuilder();
         appendPathString(pb, s);
         return pb.result();
      } else {
         return null;
      }
   }

   static class Element {
      StringBuilder sb;
      boolean canBeEmpty;

      Element(String initial, boolean canBeEmpty) {
         this.canBeEmpty = canBeEmpty;
         this.sb = new StringBuilder(initial);
      }

      @Override
      public String toString() {
         return "Element(" + this.sb.toString() + "," + this.canBeEmpty + ")";
      }
   }

   private static final class ParseContext {
      private int lineNumber = 1;
      private final Stack<Parser.TokenWithComments> buffer = new Stack<>();
      private final Iterator<Token> tokens;
      private final FullIncluder includer;
      private final ConfigIncludeContext includeContext;
      private final ConfigSyntax flavor;
      private final ConfigOrigin baseOrigin;
      private final LinkedList<Path> pathStack;
      int equalsCount;

      ParseContext(ConfigSyntax flavor, ConfigOrigin origin, Iterator<Token> tokens, FullIncluder includer, ConfigIncludeContext includeContext) {
         this.tokens = tokens;
         this.flavor = flavor;
         this.baseOrigin = origin;
         this.includer = includer;
         this.includeContext = includeContext;
         this.pathStack = new LinkedList<>();
         this.equalsCount = 0;
      }

      private static boolean attractsTrailingComments(Token token) {
         return !Tokens.isNewline(token) && token != Tokens.START && token != Tokens.OPEN_CURLY && token != Tokens.OPEN_SQUARE && token != Tokens.END;
      }

      private static boolean attractsLeadingComments(Token token) {
         return !Tokens.isNewline(token) && token != Tokens.START && token != Tokens.CLOSE_CURLY && token != Tokens.CLOSE_SQUARE && token != Tokens.END;
      }

      private void consolidateCommentBlock(Token commentToken) {
         List<Token> newlines = new ArrayList<>();
         List<Token> comments = new ArrayList<>();
         Token previous = null;
         Token next = commentToken;

         while (true) {
            if (Tokens.isNewline(next)) {
               if (previous != null && Tokens.isNewline(previous)) {
                  comments.clear();
               }

               newlines.add(next);
            } else {
               if (!Tokens.isComment(next)) {
                  if (!attractsLeadingComments(next)) {
                     comments.clear();
                  }

                  this.buffer.push(new Parser.TokenWithComments(next, comments));
                  ListIterator<Token> li = newlines.listIterator(newlines.size());

                  while (li.hasPrevious()) {
                     this.buffer.push(new Parser.TokenWithComments(li.previous()));
                  }

                  return;
               }

               comments.add(next);
            }

            previous = next;
            next = this.tokens.next();
         }
      }

      private Parser.TokenWithComments popTokenWithoutTrailingComment() {
         if (this.buffer.isEmpty()) {
            Token t = this.tokens.next();
            if (Tokens.isComment(t)) {
               this.consolidateCommentBlock(t);
               return this.buffer.pop();
            } else {
               return new Parser.TokenWithComments(t);
            }
         } else {
            return this.buffer.pop();
         }
      }

      private Parser.TokenWithComments popToken() {
         Parser.TokenWithComments withPrecedingComments = this.popTokenWithoutTrailingComment();
         if (!attractsTrailingComments(withPrecedingComments.token)) {
            return withPrecedingComments;
         } else if (this.buffer.isEmpty()) {
            Token after = this.tokens.next();
            if (Tokens.isComment(after)) {
               return withPrecedingComments.add(after);
            } else {
               this.buffer.push(new Parser.TokenWithComments(after));
               return withPrecedingComments;
            }
         } else if (Tokens.isComment(this.buffer.peek().token)) {
            throw new ConfigException.BugOrBroken("comment token should not have been in buffer: " + this.buffer);
         } else {
            return withPrecedingComments;
         }
      }

      private Parser.TokenWithComments nextToken() {
         Parser.TokenWithComments withComments = null;
         withComments = this.popToken();
         Token t = withComments.token;
         if (Tokens.isProblem(t)) {
            ConfigOrigin origin = t.origin();
            String message = Tokens.getProblemMessage(t);
            Throwable cause = Tokens.getProblemCause(t);
            boolean suggestQuotes = Tokens.getProblemSuggestQuotes(t);
            if (suggestQuotes) {
               message = this.addQuoteSuggestion(t.toString(), message);
            } else {
               message = this.addKeyName(message);
            }

            throw new ConfigException.Parse(origin, message, cause);
         } else {
            if (this.flavor == ConfigSyntax.JSON) {
               if (Tokens.isUnquotedText(t)) {
                  throw this.parseError(this.addKeyName("Token not allowed in valid JSON: '" + Tokens.getUnquotedText(t) + "'"));
               }

               if (Tokens.isSubstitution(t)) {
                  throw this.parseError(this.addKeyName("Substitutions (${} syntax) not allowed in JSON"));
               }
            }

            return withComments;
         }
      }

      private void putBack(Parser.TokenWithComments token) {
         if (Tokens.isComment(token.token)) {
            throw new ConfigException.BugOrBroken("comment token should have been stripped before it was available to put back");
         } else {
            this.buffer.push(token);
         }
      }

      private Parser.TokenWithComments nextTokenIgnoringNewline() {
         Parser.TokenWithComments t;
         for (t = this.nextToken(); Tokens.isNewline(t.token); t = this.nextToken()) {
            this.lineNumber = t.token.lineNumber() + 1;
         }

         int newNumber = t.token.lineNumber();
         if (newNumber >= 0) {
            this.lineNumber = newNumber;
         }

         return t;
      }

      private AbstractConfigValue addAnyCommentsAfterAnyComma(AbstractConfigValue v) {
         Parser.TokenWithComments t = this.nextToken();
         if (t.token == Tokens.COMMA) {
            this.putBack(t.removeAll());
            return v.withOrigin(t.appendComments(v.origin()));
         } else {
            this.putBack(t);
            return v;
         }
      }

      private boolean checkElementSeparator() {
         if (this.flavor == ConfigSyntax.JSON) {
            Parser.TokenWithComments t = this.nextTokenIgnoringNewline();
            if (t.token == Tokens.COMMA) {
               return true;
            } else {
               this.putBack(t);
               return false;
            }
         } else {
            boolean sawSeparatorOrNewline = false;

            Parser.TokenWithComments t;
            for (t = this.nextToken(); Tokens.isNewline(t.token); t = this.nextToken()) {
               this.lineNumber = t.token.lineNumber() + 1;
               sawSeparatorOrNewline = true;
            }

            if (t.token == Tokens.COMMA) {
               return true;
            } else {
               this.putBack(t);
               return sawSeparatorOrNewline;
            }
         }
      }

      private static SubstitutionExpression tokenToSubstitutionExpression(Token valueToken) {
         List<Token> expression = Tokens.getSubstitutionPathExpression(valueToken);
         Path path = Parser.parsePathExpression(expression.iterator(), valueToken.origin());
         boolean optional = Tokens.getSubstitutionOptional(valueToken);
         return new SubstitutionExpression(path, optional);
      }

      private void consolidateValueTokens() {
         if (this.flavor != ConfigSyntax.JSON) {
            List<AbstractConfigValue> values = null;
            Parser.TokenWithComments t = this.nextTokenIgnoringNewline();

            while (true) {
               AbstractConfigValue v = null;
               if (!Tokens.isValue(t.token)
                  && !Tokens.isUnquotedText(t.token)
                  && !Tokens.isSubstitution(t.token)
                  && t.token != Tokens.OPEN_CURLY
                  && t.token != Tokens.OPEN_SQUARE) {
                  this.putBack(t);
                  if (values == null) {
                     return;
                  }

                  v = ConfigConcatenation.concatenate(values);
                  this.putBack(new Parser.TokenWithComments(Tokens.newValue(v)));
                  return;
               }

               v = this.parseValue(t);
               if (v == null) {
                  throw new ConfigException.BugOrBroken("no value");
               }

               if (values == null) {
                  values = new ArrayList<>();
               }

               values.add(v);
               t = this.nextToken();
            }
         }
      }

      private SimpleConfigOrigin lineOrigin() {
         return ((SimpleConfigOrigin)this.baseOrigin).setLineNumber(this.lineNumber);
      }

      private ConfigException parseError(String message) {
         return this.parseError(message, null);
      }

      private ConfigException parseError(String message, Throwable cause) {
         return new ConfigException.Parse(this.lineOrigin(), message, cause);
      }

      private String previousFieldName(Path lastPath) {
         if (lastPath != null) {
            return lastPath.render();
         } else {
            return this.pathStack.isEmpty() ? null : this.pathStack.peek().render();
         }
      }

      private Path fullCurrentPath() {
         Path full = null;

         for (Path p : this.pathStack) {
            if (full == null) {
               full = p;
            } else {
               full = full.prepend(p);
            }
         }

         return full;
      }

      private String previousFieldName() {
         return this.previousFieldName(null);
      }

      private String addKeyName(String message) {
         String previousFieldName = this.previousFieldName();
         return previousFieldName != null ? "in value for key '" + previousFieldName + "': " + message : message;
      }

      private String addQuoteSuggestion(String badToken, String message) {
         return this.addQuoteSuggestion(null, this.equalsCount > 0, badToken, message);
      }

      private String addQuoteSuggestion(Path lastPath, boolean insideEquals, String badToken, String message) {
         String previousFieldName = this.previousFieldName(lastPath);
         String part;
         if (badToken.equals(Tokens.END.toString())) {
            if (previousFieldName == null) {
               return message;
            }

            part = message
               + " (if you intended '"
               + previousFieldName
               + "' to be part of a value, instead of a key, "
               + "try adding double quotes around the whole value";
         } else if (previousFieldName != null) {
            part = message
               + " (if you intended "
               + badToken
               + " to be part of the value for '"
               + previousFieldName
               + "', "
               + "try enclosing the value in double quotes";
         } else {
            part = message + " (if you intended " + badToken + " to be part of a key or string value, " + "try enclosing the key or value in double quotes";
         }

         return insideEquals ? part + ", or you may be able to rename the file .properties rather than .conf)" : part + ")";
      }

      private AbstractConfigValue parseValue(Parser.TokenWithComments t) {
         AbstractConfigValue v;
         if (Tokens.isValue(t.token)) {
            v = Tokens.getValue(t.token);
         } else if (Tokens.isUnquotedText(t.token)) {
            v = new ConfigString(t.token.origin(), Tokens.getUnquotedText(t.token));
         } else if (Tokens.isSubstitution(t.token)) {
            v = new ConfigReference(t.token.origin(), tokenToSubstitutionExpression(t.token));
         } else if (t.token == Tokens.OPEN_CURLY) {
            v = this.parseObject(true);
         } else {
            if (t.token != Tokens.OPEN_SQUARE) {
               throw this.parseError(this.addQuoteSuggestion(t.token.toString(), "Expecting a value but got wrong token: " + t.token));
            }

            v = this.parseArray();
         }

         return v.withOrigin(t.prependComments(v.origin()));
      }

      private static AbstractConfigObject createValueUnderPath(Path path, AbstractConfigValue value) {
         List<String> keys = new ArrayList<>();
         String key = path.first();

         for (Path remaining = path.remainder(); key != null; remaining = remaining.remainder()) {
            keys.add(key);
            if (remaining == null) {
               break;
            }

            key = remaining.first();
         }

         ListIterator<String> i = keys.listIterator(keys.size());
         String deepest = i.previous();
         AbstractConfigObject o = new SimpleConfigObject(value.origin().setComments(null), Collections.singletonMap(deepest, value));

         while (i.hasPrevious()) {
            Map<String, AbstractConfigValue> m = Collections.singletonMap(i.previous(), o);
            o = new SimpleConfigObject(value.origin().setComments(null), m);
         }

         return o;
      }

      private Path parseKey(Parser.TokenWithComments token) {
         if (this.flavor == ConfigSyntax.JSON) {
            if (Tokens.isValueWithType(token.token, ConfigValueType.STRING)) {
               String key = (String)Tokens.getValue(token.token).unwrapped();
               return Path.newKey(key);
            } else {
               throw this.parseError(this.addKeyName("Expecting close brace } or a field name here, got " + token));
            }
         } else {
            List<Token> expression = new ArrayList<>();

            Parser.TokenWithComments t;
            for (t = token; Tokens.isValue(t.token) || Tokens.isUnquotedText(t.token); t = this.nextToken()) {
               expression.add(t.token);
            }

            if (expression.isEmpty()) {
               throw this.parseError(this.addKeyName("expecting a close brace or a field name here, got " + t));
            } else {
               this.putBack(t);
               return Parser.parsePathExpression(expression.iterator(), this.lineOrigin());
            }
         }
      }

      private static boolean isIncludeKeyword(Token t) {
         return Tokens.isUnquotedText(t) && Tokens.getUnquotedText(t).equals("include");
      }

      private static boolean isUnquotedWhitespace(Token t) {
         if (!Tokens.isUnquotedText(t)) {
            return false;
         } else {
            String s = Tokens.getUnquotedText(t);

            for (int i = 0; i < s.length(); i++) {
               char c = s.charAt(i);
               if (!ConfigImplUtil.isWhitespace(c)) {
                  return false;
               }
            }

            return true;
         }
      }

      private void parseInclude(Map<String, AbstractConfigValue> values) {
         Parser.TokenWithComments t = this.nextTokenIgnoringNewline();

         while (isUnquotedWhitespace(t.token)) {
            t = this.nextTokenIgnoringNewline();
         }

         AbstractConfigObject obj;
         if (!Tokens.isUnquotedText(t.token)) {
            if (!Tokens.isValueWithType(t.token, ConfigValueType.STRING)) {
               throw this.parseError("include keyword is not followed by a quoted string, but by: " + t);
            }

            String name = (String)Tokens.getValue(t.token).unwrapped();
            obj = (AbstractConfigObject)this.includer.include(this.includeContext, name);
         } else {
            String kind = Tokens.getUnquotedText(t.token);
            if (!kind.equals("url(") && !kind.equals("file(") && !kind.equals("classpath(")) {
               throw this.parseError(
                  "expecting include parameter to be quoted filename, file(), classpath(), or url(). No spaces are allowed before the open paren. Not expecting: "
                     + t
               );
            }

            t = this.nextTokenIgnoringNewline();

            while (isUnquotedWhitespace(t.token)) {
               t = this.nextTokenIgnoringNewline();
            }

            if (!Tokens.isValueWithType(t.token, ConfigValueType.STRING)) {
               throw this.parseError("expecting a quoted string inside file(), classpath(), or url(), rather than: " + t);
            }

            String name = (String)Tokens.getValue(t.token).unwrapped();
            t = this.nextTokenIgnoringNewline();

            while (isUnquotedWhitespace(t.token)) {
               t = this.nextTokenIgnoringNewline();
            }

            if (!Tokens.isUnquotedText(t.token) || !Tokens.getUnquotedText(t.token).equals(")")) {
               throw this.parseError("expecting a close parentheses ')' here, not: " + t);
            }

            if (kind.equals("url(")) {
               URL url;
               try {
                  url = new URL(name);
               } catch (MalformedURLException var8) {
                  throw this.parseError("include url() specifies an invalid URL: " + name, var8);
               }

               obj = (AbstractConfigObject)this.includer.includeURL(this.includeContext, url);
            } else if (kind.equals("file(")) {
               obj = (AbstractConfigObject)this.includer.includeFile(this.includeContext, new File(name));
            } else {
               if (!kind.equals("classpath(")) {
                  throw new ConfigException.BugOrBroken("should not be reached");
               }

               obj = (AbstractConfigObject)this.includer.includeResources(this.includeContext, name);
            }
         }

         if (!this.pathStack.isEmpty()) {
            Path prefix = new Path(this.pathStack);
            obj = obj.relativized(prefix);
         }

         for (String key : obj.keySet()) {
            AbstractConfigValue v = obj.get(key);
            AbstractConfigValue existing = values.get(key);
            if (existing != null) {
               values.put(key, v.withFallback(existing));
            } else {
               values.put(key, v);
            }
         }
      }

      private boolean isKeyValueSeparatorToken(Token t) {
         return this.flavor == ConfigSyntax.JSON ? t == Tokens.COLON : t == Tokens.COLON || t == Tokens.EQUALS || t == Tokens.PLUS_EQUALS;
      }

      private AbstractConfigObject parseObject(boolean hadOpenCurly) {
         Map<String, AbstractConfigValue> values = new HashMap<>();
         SimpleConfigOrigin objectOrigin = this.lineOrigin();
         boolean afterComma = false;
         Path lastPath = null;
         boolean lastInsideEquals = false;

         while (true) {
            Parser.TokenWithComments t = this.nextTokenIgnoringNewline();
            if (t.token == Tokens.CLOSE_CURLY) {
               if (this.flavor == ConfigSyntax.JSON && afterComma) {
                  throw this.parseError(this.addQuoteSuggestion(t.toString(), "expecting a field name after a comma, got a close brace } instead"));
               }

               if (!hadOpenCurly) {
                  throw this.parseError(this.addQuoteSuggestion(t.toString(), "unbalanced close brace '}' with no open brace"));
               }

               objectOrigin = t.appendComments(objectOrigin);
               break;
            }

            if (t.token == Tokens.END && !hadOpenCurly) {
               this.putBack(t);
               break;
            }

            if (this.flavor != ConfigSyntax.JSON && isIncludeKeyword(t.token)) {
               this.parseInclude(values);
               afterComma = false;
            } else {
               Path path = this.parseKey(t);
               Parser.TokenWithComments afterKey = this.nextTokenIgnoringNewline();
               boolean insideEquals = false;
               this.pathStack.push(path);
               Parser.TokenWithComments valueToken;
               if (this.flavor == ConfigSyntax.CONF && afterKey.token == Tokens.OPEN_CURLY) {
                  valueToken = afterKey;
               } else {
                  if (!this.isKeyValueSeparatorToken(afterKey.token)) {
                     throw this.parseError(
                        this.addQuoteSuggestion(afterKey.toString(), "Key '" + path.render() + "' may not be followed by token: " + afterKey)
                     );
                  }

                  if (afterKey.token == Tokens.EQUALS) {
                     insideEquals = true;
                     this.equalsCount++;
                  }

                  this.consolidateValueTokens();
                  valueToken = this.nextTokenIgnoringNewline();
                  valueToken = valueToken.prepend(afterKey.comments);
               }

               AbstractConfigValue newValue = this.parseValue(valueToken.prepend(t.comments));
               if (afterKey.token == Tokens.PLUS_EQUALS) {
                  List<AbstractConfigValue> concat = new ArrayList<>(2);
                  AbstractConfigValue previousRef = new ConfigReference(newValue.origin(), new SubstitutionExpression(this.fullCurrentPath(), true));
                  AbstractConfigValue list = new SimpleConfigList(newValue.origin(), Collections.singletonList(newValue));
                  concat.add(previousRef);
                  concat.add(list);
                  newValue = ConfigConcatenation.concatenate(concat);
               }

               newValue = this.addAnyCommentsAfterAnyComma(newValue);
               lastPath = this.pathStack.pop();
               if (insideEquals) {
                  this.equalsCount--;
               }

               lastInsideEquals = insideEquals;
               String key = path.first();
               Path remaining = path.remainder();
               if (remaining == null) {
                  AbstractConfigValue existing = values.get(key);
                  if (existing != null) {
                     if (this.flavor == ConfigSyntax.JSON) {
                        throw this.parseError("JSON does not allow duplicate fields: '" + key + "' was already seen at " + existing.origin().description());
                     }

                     newValue = newValue.withFallback(existing);
                  }

                  values.put(key, newValue);
               } else {
                  if (this.flavor == ConfigSyntax.JSON) {
                     throw new ConfigException.BugOrBroken("somehow got multi-element path in JSON mode");
                  }

                  AbstractConfigObject obj = createValueUnderPath(remaining, newValue);
                  AbstractConfigValue existing = values.get(key);
                  if (existing != null) {
                     obj = obj.withFallback(existing);
                  }

                  values.put(key, obj);
               }

               afterComma = false;
            }

            if (!this.checkElementSeparator()) {
               t = this.nextTokenIgnoringNewline();
               if (t.token == Tokens.CLOSE_CURLY) {
                  if (!hadOpenCurly) {
                     throw this.parseError(this.addQuoteSuggestion(lastPath, lastInsideEquals, t.toString(), "unbalanced close brace '}' with no open brace"));
                  }

                  objectOrigin = t.appendComments(objectOrigin);
               } else {
                  if (hadOpenCurly) {
                     throw this.parseError(this.addQuoteSuggestion(lastPath, lastInsideEquals, t.toString(), "Expecting close brace } or a comma, got " + t));
                  }

                  if (t.token != Tokens.END) {
                     throw this.parseError(this.addQuoteSuggestion(lastPath, lastInsideEquals, t.toString(), "Expecting end of input or a comma, got " + t));
                  }

                  this.putBack(t);
               }
               break;
            }

            afterComma = true;
         }

         return new SimpleConfigObject(objectOrigin, values);
      }

      private SimpleConfigList parseArray() {
         SimpleConfigOrigin arrayOrigin = this.lineOrigin();
         List<AbstractConfigValue> values = new ArrayList<>();
         this.consolidateValueTokens();
         Parser.TokenWithComments t = this.nextTokenIgnoringNewline();
         if (t.token == Tokens.CLOSE_SQUARE) {
            return new SimpleConfigList(t.appendComments(arrayOrigin), Collections.emptyList());
         } else if (!Tokens.isValue(t.token) && t.token != Tokens.OPEN_CURLY && t.token != Tokens.OPEN_SQUARE) {
            throw this.parseError(
               this.addKeyName(
                  "List should have ] or a first element after the open [, instead had token: "
                     + t
                     + " (if you want "
                     + t
                     + " to be part of a string value, then double-quote it)"
               )
            );
         } else {
            AbstractConfigValue v = this.parseValue(t);
            v = this.addAnyCommentsAfterAnyComma(v);
            values.add(v);

            while (this.checkElementSeparator()) {
               this.consolidateValueTokens();
               t = this.nextTokenIgnoringNewline();
               if (!Tokens.isValue(t.token) && t.token != Tokens.OPEN_CURLY && t.token != Tokens.OPEN_SQUARE) {
                  if (this.flavor == ConfigSyntax.JSON || t.token != Tokens.CLOSE_SQUARE) {
                     throw this.parseError(
                        this.addKeyName(
                           "List should have had new element after a comma, instead had token: "
                              + t
                              + " (if you want the comma or "
                              + t
                              + " to be part of a string value, then double-quote it)"
                        )
                     );
                  }

                  this.putBack(t);
               } else {
                  v = this.parseValue(t);
                  v = this.addAnyCommentsAfterAnyComma(v);
                  values.add(v);
               }
            }

            t = this.nextTokenIgnoringNewline();
            if (t.token == Tokens.CLOSE_SQUARE) {
               return new SimpleConfigList(t.appendComments(arrayOrigin), values);
            } else {
               throw this.parseError(
                  this.addKeyName(
                     "List should have ended with ] or had a comma, instead had token: "
                        + t
                        + " (if you want "
                        + t
                        + " to be part of a string value, then double-quote it)"
                  )
               );
            }
         }
      }

      AbstractConfigValue parse() {
         Parser.TokenWithComments t = this.nextTokenIgnoringNewline();
         if (t.token != Tokens.START) {
            throw new ConfigException.BugOrBroken("token stream did not begin with START, had " + t);
         } else {
            t = this.nextTokenIgnoringNewline();
            AbstractConfigValue result = null;
            if (t.token != Tokens.OPEN_CURLY && t.token != Tokens.OPEN_SQUARE) {
               if (this.flavor == ConfigSyntax.JSON) {
                  if (t.token == Tokens.END) {
                     throw this.parseError("Empty document");
                  }

                  throw this.parseError("Document must have an object or array at root, unexpected token: " + t);
               }

               this.putBack(t);
               result = this.parseObject(false);
            } else {
               result = this.parseValue(t);
            }

            t = this.nextTokenIgnoringNewline();
            if (t.token == Tokens.END) {
               return result;
            } else {
               throw this.parseError("Document has trailing tokens after first object or array: " + t);
            }
         }
      }
   }

   private static final class TokenWithComments {
      final Token token;
      final List<Token> comments;

      TokenWithComments(Token token, List<Token> comments) {
         this.token = token;
         this.comments = comments;
         if (Tokens.isComment(token)) {
            throw new ConfigException.BugOrBroken("tried to annotate a comment with a comment");
         }
      }

      TokenWithComments(Token token) {
         this(token, Collections.emptyList());
      }

      Parser.TokenWithComments removeAll() {
         return this.comments.isEmpty() ? this : new Parser.TokenWithComments(this.token);
      }

      Parser.TokenWithComments prepend(List<Token> earlier) {
         if (earlier.isEmpty()) {
            return this;
         } else if (this.comments.isEmpty()) {
            return new Parser.TokenWithComments(this.token, earlier);
         } else {
            List<Token> merged = new ArrayList<>();
            merged.addAll(earlier);
            merged.addAll(this.comments);
            return new Parser.TokenWithComments(this.token, merged);
         }
      }

      Parser.TokenWithComments add(Token after) {
         if (this.comments.isEmpty()) {
            return new Parser.TokenWithComments(this.token, Collections.singletonList(after));
         } else {
            List<Token> merged = new ArrayList<>();
            merged.addAll(this.comments);
            merged.add(after);
            return new Parser.TokenWithComments(this.token, merged);
         }
      }

      SimpleConfigOrigin prependComments(SimpleConfigOrigin origin) {
         if (this.comments.isEmpty()) {
            return origin;
         } else {
            List<String> newComments = new ArrayList<>();

            for (Token c : this.comments) {
               newComments.add(Tokens.getCommentText(c));
            }

            return origin.prependComments(newComments);
         }
      }

      SimpleConfigOrigin appendComments(SimpleConfigOrigin origin) {
         if (this.comments.isEmpty()) {
            return origin;
         } else {
            List<String> newComments = new ArrayList<>();

            for (Token c : this.comments) {
               newComments.add(Tokens.getCommentText(c));
            }

            return origin.appendComments(newComments);
         }
      }

      @Override
      public String toString() {
         return this.token.toString();
      }
   }
}
