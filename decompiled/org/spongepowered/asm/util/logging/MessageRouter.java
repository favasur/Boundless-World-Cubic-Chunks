package org.spongepowered.asm.util.logging;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MessageRouter {
   private static Messager messager;

   private MessageRouter() {
   }

   public static Messager getMessager() {
      if (messager == null) {
         messager = new MessageRouter.LoggingMessager();
      }

      return messager;
   }

   public static void setMessager(Messager messager) {
      MessageRouter.messager = messager == null ? null : new MessageRouter.DebugInterceptingMessager(messager);
   }

   static class DebugInterceptingMessager implements Messager {
      private final Messager wrapped;

      DebugInterceptingMessager(Messager messager) {
         this.wrapped = messager;
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg);
         }
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e);
         }
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e, a);
         }
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
         if (kind != Kind.OTHER) {
            this.wrapped.printMessage(kind, msg, e, a, v);
         }
      }
   }

   static class LoggingMessager implements Messager {
      private static final Logger logger = LogManager.getLogger("mixin");

      LoggingMessager() {
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg) {
         logger.log(messageKindToLoggingLevel(kind), msg);
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e) {
         logger.log(messageKindToLoggingLevel(kind), msg);
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
         logger.log(messageKindToLoggingLevel(kind), msg);
      }

      @Override
      public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
         logger.log(messageKindToLoggingLevel(kind), msg);
      }

      private static Level messageKindToLoggingLevel(Kind kind) {
         switch (kind) {
            case ERROR:
               return Level.ERROR;
            case WARNING:
            case MANDATORY_WARNING:
               return Level.WARN;
            case NOTE:
               return Level.INFO;
            case OTHER:
            default:
               return Level.DEBUG;
         }
      }
   }
}
