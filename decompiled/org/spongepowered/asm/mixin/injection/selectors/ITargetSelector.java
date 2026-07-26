package org.spongepowered.asm.mixin.injection.selectors;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.ElementNode;

public interface ITargetSelector {
   ITargetSelector next();

   ITargetSelector configure(String... var1);

   ITargetSelector validate() throws InvalidSelectorException;

   ITargetSelector attach(IMixinContext var1) throws InvalidSelectorException;

   int getMatchCount();

   <TNode> MatchResult match(ElementNode<TNode> var1);

   MatchResult match(AbstractInsnNode var1);
}
