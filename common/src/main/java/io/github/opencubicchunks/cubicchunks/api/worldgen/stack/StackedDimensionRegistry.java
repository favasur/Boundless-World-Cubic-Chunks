package io.github.opencubicchunks.cubicchunks.api.worldgen.stack;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global registry of stacked sub-dimensions that are merged into the overworld's
 * cube provider. Mods / configs may register custom sub-dims at boot; the overworld
 * MixinServerLevel hook pushes the registered sub-dims into the {@link
 * io.github.opencubicchunks.cubicchunks.core.worldgen.stack.StackedCubeGenerator}.
 *
 * <p>Stacking is opt-in: if no sub-dim is registered, the cube provider behaves as a
 * regular cubic overworld.</p>
 */
public final class StackedDimensionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackedDimensionRegistry.class);

    private static final ConcurrentMap<ResourceLocation, StackedDimension> ENTRIES = new ConcurrentHashMap<>();
    private static volatile boolean DEFAULT_BOOTED = false;

    private StackedDimensionRegistry() {
    }

    /**
     * Registers a stacked sub-dim. Replaces any existing entry with the same id.
     * Categories of conflict: range overlap with already-registered sub-dim.
     */
    public static void register(StackedDimension dim) {
        if (dim == null) {
            return;
        }
        for (StackedDimension existing : ENTRIES.values()) {
            if (existing == dim) {
                continue;
            }
            if (rangesOverlap(existing, dim)) {
                LOGGER.warn("Stacked sub-dim {} overlaps with already-registered {}; ignoring.",
                        dim.id(), existing.id());
                return;
            }
        }
        ENTRIES.put(dim.id(), dim);
        LOGGER.info("Registered stacked sub-dim {} [{}..{}] with palette ambient={}",
                dim.id(), dim.minBlockY(), dim.maxBlockY(),
                dim.palette().ambientLight());
    }

    /**
     * Removes a stacked sub-dim by id. Returns true if something was removed.
     */
    public static boolean unregister(ResourceLocation id) {
        boolean removed = ENTRIES.remove(id) != null;
        if (removed) {
            LOGGER.info("Unregistered stacked sub-dim {}", id);
        }
        return removed;
    }

    public static Optional<StackedDimension> get(ResourceLocation id) {
        return Optional.ofNullable(ENTRIES.get(id));
    }

    public static List<StackedDimension> all() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));
    }

    public static int size() {
        return ENTRIES.size();
    }

    /**
     * Returns the stacked sub-dim that owns the given cube Y. If no sub-dim is
     * registered for this Y, returns empty rather than falling through to the active
     * dimension's vanilla window.
     */
    public static Optional<StackedDimension> findForCubeY(int cubeY) {
        for (StackedDimension d : ENTRIES.values()) {
            if (d.containsCubeY(cubeY)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the highest stacked sub-dim whose Y range is entirely below
     * {@code blockY}. Useful for cube providers to figure out "what band am I in"
     * when the player crosses a boundary. Linear scan over registered entries; for
     * the MVP the registry size is ≤ the default three so a scan is fine.
     */
    public static Optional<StackedDimension> stackedBelow(int cubeY) {
        StackedDimension best = null;
        for (StackedDimension d : ENTRIES.values()) {
            if (d.getMaxCubeY() < cubeY && (best == null || d.getMaxCubeY() > best.getMaxCubeY())) {
                best = d;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Boots the default overworld/nether/end stacked sub-dims. Called once from the
     * loader entrypoints' {@code commonSetup} phase. {@code bootDefaultsIfNeeded} is
     * idempotent.
     */
    public static synchronized void bootDefaultsIfNeeded() {
        if (DEFAULT_BOOTED) {
            return;
        }
        DEFAULT_BOOTED = true;
        register(StackedDimensions.OVERWORLD);
        register(StackedDimensions.NETHER);
        register(StackedDimensions.END);
    }

    /**
     * Resets the registry.  Test-only — production code must never call this.
     * Visible (not private) so JUnit can reach it from the test sourceset; callers
     * in production code are forbidden because it would wipe the default
     * Nether / End / Overworld entries that {@link #bootDefaultsIfNeeded} added.
     */
    @VisibleForTesting
    static synchronized void resetForTests() {
        ENTRIES.clear();
        DEFAULT_BOOTED = false;
    }

    /**
     * Marker annotation mirroring the visibility hint of forge's
     * {@code com.google.common.annotations.VisibleForTesting}. We don't import
     * guava just for this annotation; the source-level comment is enough.
     */
    @interface VisibleForTesting {
    }

    private static boolean rangesOverlap(StackedDimension a, StackedDimension b) {
        return a.minBlockY() <= b.maxBlockY() && b.minBlockY() <= a.maxBlockY();
    }

    /**
     * Convenience: returns the sub-dim whose id matches the resource-location form of
     * the active dimension key, or empty if no stacked sub-dim owns that id.
     */
    @Nullable
    public static StackedDimension findById(ResourceLocation id) {
        return ENTRIES.get(id);
    }
}
