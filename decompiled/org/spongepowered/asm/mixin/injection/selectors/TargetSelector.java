package org.spongepowered.asm.mixin.injection.selectors;

import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.util.asm.ElementNode;

public final class TargetSelector {
   private TargetSelector() {
   }

   public static ITargetSelector parseAndValidate(String string) throws InvalidMemberDescriptorException {
      return parse(string, null, null).validate();
   }

   public static ITargetSelector parseAndValidate(String string, IMixinContext context) throws InvalidMemberDescriptorException {
      return parse(string, context.getReferenceMapper(), context.getClassRef()).validate();
   }

   public static ITargetSelector parse(String string) {
      return parse(string, null, null);
   }

   public static ITargetSelector parse(String string, IMixinContext context) {
      return parse(string, context.getReferenceMapper(), context.getClassRef());
   }

   public static String parseName(String name, IMixinContext context) {
      ITargetSelector selector = parse(name, context);
      if (!(selector instanceof ITargetSelectorByName)) {
         return name;
      } else {
         String mappedName = ((ITargetSelectorByName)selector).getName();
         return mappedName != null ? mappedName : name;
      }
   }

   private static ITargetSelector parse(String input, IReferenceMapper refMapper, String mixinClass) {
      return MemberInfo.parse(input, refMapper, mixinClass);
   }

   public static <TNode> TargetSelector.Result<TNode> run(ITargetSelector target, List<ElementNode<TNode>> nodes) {
      List<TNode> candidates = new ArrayList<>();
      TNode exactMatch = runSelector(target, nodes, candidates);
      return new TargetSelector.Result<>(exactMatch, candidates);
   }

   public static <TNode> TargetSelector.Result<TNode> run(Iterable<ITargetSelector> targets, List<ElementNode<TNode>> nodes) {
      TNode exactMatch = null;
      List<TNode> candidates = new ArrayList<>();

      for (ITargetSelector target : targets) {
         TNode selectorExactMatch = runSelector(target, nodes, candidates);
         if (exactMatch == null) {
            exactMatch = selectorExactMatch;
         }
      }

      return new TargetSelector.Result<>(exactMatch, candidates);
   }

   private static <TNode> TNode runSelector(ITargetSelector target, List<ElementNode<TNode>> nodes, List<TNode> candidates) {
      TNode exactMatch = null;

      for (ElementNode<TNode> element : nodes) {
         MatchResult match = target.match(element);
         if (match.isMatch()) {
            TNode node = element.get();
            if (!candidates.contains(node)) {
               candidates.add(node);
            }

            if (exactMatch == null && match.isExactMatch()) {
               exactMatch = node;
            }
         }
      }

      return exactMatch;
   }

   public static class Result<TNode> {
      public final TNode exactMatch;
      public final List<TNode> candidates;

      Result(TNode exactMatch, List<TNode> candidates) {
         this.exactMatch = exactMatch;
         this.candidates = candidates;
      }

      public TNode getSingleResult(boolean strict) {
         int resultCount = this.candidates.size();
         if (this.exactMatch != null) {
            return this.exactMatch;
         } else if (resultCount != 1 && strict) {
            throw new IllegalStateException((resultCount == 0 ? "No" : "Multiple") + " candidates were found");
         } else {
            return this.candidates.get(0);
         }
      }
   }
}
