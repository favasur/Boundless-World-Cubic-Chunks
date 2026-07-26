package com.typesafe.config.impl;

final class SubstitutionExpression {
   private final Path path;
   private final boolean optional;

   SubstitutionExpression(Path path, boolean optional) {
      this.path = path;
      this.optional = optional;
   }

   Path path() {
      return this.path;
   }

   boolean optional() {
      return this.optional;
   }

   SubstitutionExpression changePath(Path newPath) {
      return newPath == this.path ? this : new SubstitutionExpression(newPath, this.optional);
   }

   @Override
   public String toString() {
      return "${" + (this.optional ? "?" : "") + this.path.render() + "}";
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof SubstitutionExpression)) {
         return false;
      } else {
         SubstitutionExpression otherExp = (SubstitutionExpression)other;
         return otherExp.path.equals(this.path) && otherExp.optional == this.optional;
      }
   }

   @Override
   public int hashCode() {
      int h = 41 * (41 + this.path.hashCode());
      return 41 * (h + (this.optional ? 1 : 0));
   }
}
