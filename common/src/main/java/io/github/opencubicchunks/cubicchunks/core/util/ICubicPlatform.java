package io.github.opencubicchunks.cubicchunks.core.util;

import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

// @Original: 1.12.2 SideUtils; 1.21 rewrite as a loader-agnostic shim.
// Fabric & NeoForge each register an ICubicPlatform at entry-point time. The fireEvent
// hook lets common code publish gameplay events without referencing Forge's ModLoader.
public interface ICubicPlatform {

    boolean isClient();

    String sideName();

    BlockableEventLoop<?> mainThreadExecutor();

    @Nullable
    Level getClientLevel();

    /** Post an event to the loader's event bus. Implementations ignore on Fabric (we have
     * no shared gameplay event surface there yet) and forward to NeoForge's
     * {@code NeoForge.EVENT_BUS.post(event)} on NeoForge. */
    default void fireEvent(Object event) {
    }

    static ICubicPlatform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        @Nullable private static ICubicPlatform INSTANCE;

        public static void set(ICubicPlatform impl) {
            INSTANCE = impl;
        }

        @Nullable public static ICubicPlatform get() {
            return INSTANCE;
        }
    }

    static boolean isClientNullSafe() {
        ICubicPlatform impl = Holder.get();
        return impl != null && impl.isClient();
    }
}
