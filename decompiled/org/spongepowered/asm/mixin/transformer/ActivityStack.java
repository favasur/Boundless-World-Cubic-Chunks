package org.spongepowered.asm.mixin.transformer;

public class ActivityStack {
   public static final String GLUE_STRING = " -> ";
   private final ActivityStack.Activity head;
   private ActivityStack.Activity tail;
   private String glue;

   public ActivityStack() {
      this(null, " -> ");
   }

   public ActivityStack(String root) {
      this(root, " -> ");
   }

   public ActivityStack(String root, String glue) {
      this.head = this.tail = new ActivityStack.Activity(null, root);
      this.glue = glue;
   }

   public void clear() {
      this.tail = this.head;
      this.head.next = null;
   }

   public ActivityStack.Activity begin(String description) {
      return this.tail = new ActivityStack.Activity(this.tail, description != null ? description : "null");
   }

   public ActivityStack.Activity begin(String descriptionFormat, Object... args) {
      if (descriptionFormat == null) {
         descriptionFormat = "null";
      }

      return this.tail = new ActivityStack.Activity(this.tail, String.format(descriptionFormat, args));
   }

   void end(ActivityStack.Activity activity) {
      this.tail = activity.last;
      this.tail.next = null;
   }

   @Override
   public String toString() {
      return this.toString(this.glue);
   }

   public String toString(String glue) {
      if (this.head.description == null && this.head.next == null) {
         return "Unknown";
      } else {
         StringBuilder sb = new StringBuilder();

         for (ActivityStack.Activity activity = this.head; activity != null; activity = activity.next) {
            if (activity.description != null) {
               sb.append(activity.description);
               if (activity.next != null) {
                  sb.append(glue);
               }
            }
         }

         return sb.toString();
      }
   }

   public class Activity {
      public String description;
      ActivityStack.Activity last;
      ActivityStack.Activity next;

      Activity(ActivityStack.Activity last, String description) {
         if (last != null) {
            last.next = this;
         }

         this.last = last;
         this.description = description;
      }

      public void append(String text) {
         this.description = this.description != null ? this.description + text : text;
      }

      public void append(String textFormat, Object... args) {
         this.append(String.format(textFormat, args));
      }

      public void end() {
         if (this.last != null) {
            ActivityStack.this.end(this);
            this.last = null;
         }
      }

      public void next(String description) {
         if (this.next != null) {
            this.next.end();
         }

         this.description = description;
      }

      public void next(String descriptionFormat, Object... args) {
         if (descriptionFormat == null) {
            descriptionFormat = "null";
         }

         this.next(String.format(descriptionFormat, args));
      }
   }
}
