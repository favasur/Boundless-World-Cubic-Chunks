package org.spongepowered.asm.mixin.injection.struct;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyConstantInjector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(ModifyConstant.class)
@InjectionInfo.HandlerPrefix("constant")
public class ModifyConstantInjectionInfo extends InjectionInfo {
   private static final String CONSTANT_ANNOTATION_CLASS = Constant.class.getName().replace('.', '/');

   public ModifyConstantInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
      super(mixin, method, annotation, "constant");
   }

   @Override
   protected List<AnnotationNode> readInjectionPoints() {
      List<AnnotationNode> ats = super.readInjectionPoints();
      if (ats.isEmpty()) {
         AnnotationNode c = new AnnotationNode(CONSTANT_ANNOTATION_CLASS);
         c.visit("log", Boolean.TRUE);
         ats = ImmutableList.of(c);
      }

      return ats;
   }

   @Override
   protected void parseInjectionPoints(List<AnnotationNode> ats) {
      Type returnType = Type.getReturnType(this.method.desc);

      for (AnnotationNode at : ats) {
         this.injectionPoints.add(new BeforeConstant(this.getContext(), at, returnType.getDescriptor()));
      }
   }

   @Override
   protected Injector parseInjector(AnnotationNode injectAnnotation) {
      return new ModifyConstantInjector(this);
   }

   @Override
   protected String getDescription() {
      return "Constant modifier method";
   }

   @Override
   public String getSliceId(String id) {
      return Strings.nullToEmpty(id);
   }
}
