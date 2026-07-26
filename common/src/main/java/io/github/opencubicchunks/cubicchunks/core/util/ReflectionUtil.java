package io.github.opencubicchunks.cubicchunks.core.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object in) {
        return (T) in;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getClassOrDefault(String name, Class<? extends T> fallback) {
        try {
            return (Class<? extends T>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            return fallback;
        }
    }

    public static MethodHandle constructHandle(Class<?> owner, Class<?>... args) {
        try {
            Constructor<?> ctor = owner.getDeclaredConstructor(args);
            ctor.setAccessible(true);
            return MethodHandles.lookup().unreflectConstructor(ctor);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
