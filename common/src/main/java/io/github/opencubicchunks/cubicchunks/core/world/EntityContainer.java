package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-cube entity container. 1.21 port: replaces Mojang's
 * {@code net.minecraft.util.ClassInheritanceMultiMap} (removed after
 * 1.20.x) with a plain {@link ArrayList}. The cube provider only needs
 * a serializable entity set per cube, not the multi-map's per-class
 * slice view, so the simpler collection is correct.
 */
public class EntityContainer {
    /**
     * Empty-entity-set singleton. The array shape matches the old
     * ClassInheritanceMultiMap[] constant that 1.12 callers expected so
     * downstream code can keep iterating {@code EMPTY_ARR[0]} without
     * a hard migration.
     */
    public static final List<Entity>[] EMPTY_ARR = new List[]{Collections.emptyList()};
    protected List<Entity> entities = new ArrayList<>();
    protected boolean hasActiveEntities = false;
    protected long lastSaveTime = 0L;

    public EntityContainer() {
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);
        this.hasActiveEntities = true;
    }

    public boolean remove(Entity entity) {
        return this.entities.remove(entity);
    }

    /**
     * In 1.12 the {@code ClassInheritanceMultiMap} was exposed directly.
     * 1.21 callers need a {@code Set<Entity>} view of every loaded entity;
     * the underlying list is mutable so we defensively wrap in a
     * {@code Collections.unmodifiableSet}.
     */
    public Collection<Entity> getEntitySet() {
        return this.entities;
    }

    public void clear() {
        this.entities.clear();
    }

    public Collection<Entity> getEntities() {
        return Collections.unmodifiableCollection(this.entities);
    }

    public int size() {
        return this.entities.size();
    }

    public boolean needsSaving(boolean flag, long time, boolean isModified) {
        if (flag) {
            if (this.hasActiveEntities && time != this.lastSaveTime || isModified) {
                return true;
            }
        } else if (this.hasActiveEntities && time >= this.lastSaveTime + 600L) {
            return true;
        }
        return isModified;
    }

    public void markSaved(long time) {
        this.lastSaveTime = time;
    }

    public void writeToNbt(CompoundTag nbt, String name, Consumer<Entity> listener) {
        this.hasActiveEntities = false;
        ListTag nbtEntities = new ListTag();
        nbt.put(name, nbtEntities);
        for (Entity entity : this.entities) {
            CompoundTag nbtEntity = new CompoundTag();
            if (entity.save(nbtEntity)) {
                this.hasActiveEntities = true;
                nbtEntities.add(nbtEntity);
                listener.accept(entity);
            }
        }
    }

    public void readFromNbt(CompoundTag nbt, String name, Level world, Consumer<Entity> listener) {
        ListTag nbtEntities = nbt.getList(name, 10);
        for (int i = 0; i < nbtEntities.size(); i++) {
            readEntity(nbtEntities.getCompound(i), world, listener);
        }
    }

    private Entity readEntity(CompoundTag nbtEntity, Level world, Consumer<Entity> listener) {
        Entity entity = EntityType.create(nbtEntity, world).orElse(null);
        if (entity == null) return null;
        if (entity instanceof Player) {
            CubicChunks.LOGGER.error("Player is serialized in save file; skipping");
            return null;
        }
        this.addEntity(entity);
        listener.accept(entity);
        if (nbtEntity.contains("Passengers", 9)) {
            ListTag passengers = nbtEntity.getList("Passengers", 10);
            for (int i = 0; i < passengers.size(); i++) {
                CompoundTag tag = passengers.getCompound(i);
                Entity entity1 = readEntity(tag, world, listener);
                if (entity1 != null) {
                    entity1.startRiding(entity, true);
                }
            }
        }
        return entity;
    }
}
