package org.spongepowered.asm.mixin.injection.struct;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.IMessageSink;

public class InjectionPointData {
   private static final Pattern AT_PATTERN = createPattern();
   private final Map<String, String> args = new HashMap<>();
   private final IInjectionPointContext context;
   private final String at;
   private final String type;
   private final InjectionPoint.Selector selector;
   private final String target;
   private final String slice;
   private final int ordinal;
   private final int opcode;
   private final String id;

   public InjectionPointData(IInjectionPointContext context, String at, List<String> args, String target, String slice, int ordinal, int opcode, String id) {
      this.context = context;
      this.at = at;
      this.target = target;
      this.slice = Strings.nullToEmpty(slice);
      this.ordinal = Math.max(-1, ordinal);
      this.opcode = opcode;
      this.id = id;
      this.parseArgs(args);
      this.args.put("target", target);
      this.args.put("ordinal", String.valueOf(ordinal));
      this.args.put("opcode", String.valueOf(opcode));
      Matcher matcher = AT_PATTERN.matcher(at);
      this.type = parseType(matcher, at);
      this.selector = parseSelector(matcher);
   }

   private void parseArgs(List<String> args) {
      if (args != null) {
         for (String arg : args) {
            if (arg != null) {
               int eqPos = arg.indexOf(61);
               if (eqPos > -1) {
                  this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
               } else {
                  this.args.put(arg, "");
               }
            }
         }
      }
   }

   public IMessageSink getMessageSink() {
      return this.context;
   }

   public String getAt() {
      return this.at;
   }

   public String getType() {
      return this.type;
   }

   public InjectionPoint.Selector getSelector() {
      return this.selector;
   }

   public IMixinContext getContext() {
      return this.context.getContext();
   }

   public MethodNode getMethod() {
      return this.context.getMethod();
   }

   public Type getMethodReturnType() {
      return Type.getReturnType(this.getMethod().desc);
   }

   public AnnotationNode getParent() {
      return this.context.getAnnotation();
   }

   public String getSlice() {
      return this.slice;
   }

   public LocalVariableDiscriminator getLocalVariableDiscriminator() {
      return LocalVariableDiscriminator.parse(this.getParent());
   }

   public String get(String key, String defaultValue) {
      String value = this.args.get(key);
      return value != null ? value : defaultValue;
   }

   public int get(String key, int defaultValue) {
      return parseInt(this.get(key, String.valueOf(defaultValue)), defaultValue);
   }

   public boolean get(String key, boolean defaultValue) {
      return parseBoolean(this.get(key, String.valueOf(defaultValue)), defaultValue);
   }

   public ITargetSelector get(String key) {
      try {
         return TargetSelector.parseAndValidate(this.get(key, ""), this.getContext());
      } catch (InvalidMemberDescriptorException var3) {
         throw new InvalidInjectionPointException(
            this.getContext(), "Failed parsing @At(\"%s\").%s descriptor \"%s\" on %s", this.at, key, this.target, this.getDescription()
         );
      }
   }

   public ITargetSelector getTarget() {
      try {
         return TargetSelector.parseAndValidate(this.target, this.getContext());
      } catch (InvalidMemberDescriptorException var2) {
         throw new InvalidInjectionPointException(
            this.getContext(), "Failed parsing @At(\"%s\").target descriptor \"%s\" on %s", this.at, this.target, this.getDescription()
         );
      }
   }

   public String getDescription() {
      return InjectionInfo.describeInjector(this.getContext(), this.getParent(), this.getMethod());
   }

   public int getOrdinal() {
      return this.ordinal;
   }

   public int getOpcode() {
      return this.opcode;
   }

   public int getOpcode(int defaultOpcode) {
      return this.opcode > 0 ? this.opcode : defaultOpcode;
   }

   public int getOpcode(int defaultOpcode, int... validOpcodes) {
      for (int validOpcode : validOpcodes) {
         if (this.opcode == validOpcode) {
            return this.opcode;
         }
      }

      return defaultOpcode;
   }

   public String getId() {
      return this.id;
   }

   @Override
   public String toString() {
      return this.type;
   }

   private static Pattern createPattern() {
      return Pattern.compile(String.format("^([^:]+):?(%s)?$", Joiner.on('|').join(InjectionPoint.Selector.values())));
   }

   public static String parseType(String at) {
      Matcher matcher = AT_PATTERN.matcher(at);
      return parseType(matcher, at);
   }

   private static String parseType(Matcher matcher, String at) {
      return matcher.matches() ? matcher.group(1) : at;
   }

   private static InjectionPoint.Selector parseSelector(Matcher matcher) {
      return matcher.matches() && matcher.group(2) != null ? InjectionPoint.Selector.valueOf(matcher.group(2)) : InjectionPoint.Selector.DEFAULT;
   }

   private static int parseInt(String string, int defaultValue) {
      try {
         return Integer.parseInt(string);
      } catch (Exception var3) {
         return defaultValue;
      }
   }

   private static boolean parseBoolean(String string, boolean defaultValue) {
      try {
         return Boolean.parseBoolean(string);
      } catch (Exception var3) {
         return defaultValue;
      }
   }
}
