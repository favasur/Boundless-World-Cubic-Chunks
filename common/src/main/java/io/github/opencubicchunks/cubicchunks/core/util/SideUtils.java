package io.github.opencubicchunks.cubicchunks.core.util;

import java.util.function.Supplier;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.util.SideUtils
// 1.21: replaced FMLCommonHandler with loader-agnostic ICubicPlatform shim.
public final class SideUtils {

    private SideUtils() {
    }

    public static <T> T getForSide(Supplier<Supplier<T>> clientSupplier, Supplier<Supplier<T>> serverSupplier) {
        return ICubicPlatform.get().isClient() ? clientSupplier.get().get() : serverSupplier.get().get();
    }

    public static void runForSide(Supplier<Runnable> clientSide, Supplier<Runnable> serverSide) {
        if (ICubicPlatform.get().isClient()) {
            clientSide.get().run();
        } else {
            serverSide.get().run();
        }
    }

    public static void runForClient(Supplier<Runnable> toRun) {
        if (ICubicPlatform.get().isClient()) {
            toRun.get().run();
        }
    }
}
