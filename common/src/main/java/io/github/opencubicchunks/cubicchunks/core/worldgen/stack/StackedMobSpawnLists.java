package io.github.opencubicchunks.cubicchunks.core.worldgen.stack;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimension;
import io.github.opencubicchunks.cubicchunks.api.worldgen.stack.StackedDimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static per-band mob spawn lists used by {@code StackedCubeGenerator.getPossibleCreatures}.
 *
 * <p>The Nether band lists {@code blaze, piglin, hoglin, zombified_piglin, ghast}
 * across {@code MobCategory.MONSTER} and {@code MobCategory.CREATURE}; the End band
 * lists {@code enderman} across {@code MONSTER} and {@code shulker} via the
 * ARTHROPOD-alias bucket. Overworld band falls back to the existing per-biome
 * vanilla lookup because stacking only adds sub-dims below or above the overworld's
 * own Y window.</p>
 *
 * <p>The lists are keyed by entity registry name resolution at boot; if a vanilla
 * entity isn't available (e.g. a stripped-down modpack), the entry is dropped
 * silently so the rest of the list still serves.</p>
 */
public final class StackedMobSpawnLists {

    private final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> netherSpawnList = new HashMap<>();
    private final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> endSpawnList = new HashMap<>();

    private StackedMobSpawnLists() {
        fillNether();
        fillEnd();
    }

    public static StackedMobSpawnLists get() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the mob spawn list for the band owning {@code (pos)}, or null when
     * the band does not override the spawn list (e.g. the overworld band — the
     * caller falls back to the overworld biome lookup).
     */
    @Nullable
    public Map<MobCategory, List<MobSpawnSettings.SpawnerData>> spawnListFor(BlockPos pos) {
        int cubeY = Coords.blockToCube(pos.getY());
        var subDim = StackedDimensionRegistry.findForCubeY(cubeY);
        if (subDim.isEmpty()) {
            return null;
        }
        StackedDimension dim = subDim.get();
        ResourceLocation id = dim.id();
        if (id.getNamespace().equals("cubicchunks")) {
            String path = id.getPath();
            if (path.contains("nether")) return netherSpawnList;
            if (path.contains("end") || path.contains("the_end")) return endSpawnList;
        }
        return null;
    }

    private void fillNether() {
        netherSpawnList.put(MobCategory.MONSTER, List.of(
                spawnData(EntityType.BLAZE, 50),
                spawnData(EntityType.PIGLIN, 50),
                spawnData(EntityType.HOGLIN, 25),
                spawnData(EntityType.ZOMBIFIED_PIGLIN, 80),
                spawnData(EntityType.MAGMA_CUBE, 30),
                spawnData(EntityType.GHAST, 30),
                spawnData(EntityType.SKELETON, 60),
                spawnData(EntityType.WITHER_SKELETON, 12)
        ));
        netherSpawnList.put(MobCategory.CREATURE, List.of(
                spawnData(EntityType.PIGLIN_BRUTE, 1),
                spawnData(EntityType.STRIDER, 30)
        ));
    }

    private void fillEnd() {
        endSpawnList.put(MobCategory.MONSTER, List.of(
                spawnData(EntityType.ENDERMAN, 60),
                spawnData(EntityType.SHULKER, 20)
        ));
        endSpawnList.put(MobCategory.CREATURE, List.of(
                // The End has no CREATURE-class mobs by vanilla convention; empty list.
        ));
    }

    private static MobSpawnSettings.SpawnerData spawnData(EntityType<?> type, int weight) {
        return new MobSpawnSettings.SpawnerData(type, weight, 1, 4);
    }

    /** Pre-resolved "MobCategory → spawn list" lookup stored per-band. */
    public List<MobSpawnSettings.SpawnerData> get(@Nullable StackedDimension dim, MobCategory cat) {
        if (dim == null) return List.of();
        ResourceLocation id = dim.id();
        if (id.getNamespace().equals("cubicchunks")) {
            String path = id.getPath();
            if (path.contains("nether")) {
                return netherSpawnList.getOrDefault(cat, List.of());
            }
            if (path.contains("end") || path.contains("the_end")) {
                return endSpawnList.getOrDefault(cat, List.of());
            }
        }
        return List.of();
    }

    private static final class Holder {
        private static final StackedMobSpawnLists INSTANCE = new StackedMobSpawnLists();
    }
}
