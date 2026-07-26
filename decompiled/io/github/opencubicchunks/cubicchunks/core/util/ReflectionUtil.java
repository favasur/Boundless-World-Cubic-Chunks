package io.github.opencubicchunks.cubicchunks.core.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReflectionUtil {
   public ReflectionUtil() {
   }

   public static <T> T cast(Object in) {
      return (T)in;
   }

   public static <T> Class<? extends T> getClassOrDefault(String name, Class<? extends T> cl) {
      try {
         return cast(Class.forName(name));
      } catch (ClassNotFoundException var3) {
         return cl;
      }
   }

   public static MethodHandle constructHandle(Class<?> owner, Class<?>... args) {
      try {
         Constructor<?> constr = owner.getDeclaredConstructor(args);
         constr.setAccessible(true);
         return MethodHandles.lookup().unreflectConstructor(constr);
      } catch (NoSuchMethodException | IllegalAccessException var3) {
         throw new Error(var3);
      }
   }
}
