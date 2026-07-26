package io.github.opencubicchunks.cubicchunks.core.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * Empty entity container for {@code BlankCube}. 1.21 port: replaces the
 * {@code ClassInheritanceMultiMap} reference and the inner {@code BlankEntityMap}
 * with a plain read-only view of an empty list, since the BlankCube never
 * carries entities and 1.21 has no ClassInheritanceMultiMap at all.
 */
public class BlankEntityContainer extends EntityContainer {
    public BlankEntityContainer() {
        this.entities = EntityContainer.EMPTY_ARR[0];
    }

    @Override public void addEntity(Entity entity) { }
    @Override public boolean remove(Entity entity) { return false; }
    @Override public void clear() { }
    @Override public Collection<Entity> getEntities() { return Collections.emptyList(); }
    @Override public int size() { return 0; }
    @Override public boolean needsSaving(boolean flag, long time, boolean isModified) { return false; }
    @Override public void markSaved(long time) { }
    @Override public void writeToNbt(CompoundTag nbt, String name, Consumer<Entity> listener) { }
    @Override public void readFromNbt(CompoundTag nbt, String name, Level world, Consumer<Entity> listener) { }
}
