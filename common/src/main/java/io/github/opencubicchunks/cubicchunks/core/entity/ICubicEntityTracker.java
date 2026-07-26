package io.github.opencubicchunks.cubicchunks.core.entity;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySectionStorage;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker
// 1.21: the entity tracker is now a PersistentEntitySectionManager<ServerLevel>.
// ICubicEntityTracker is kept as a hook so CubeMap iteration can route per-cube
// visibility lookups without grepping the entity manager down.
public interface ICubicEntityTracker {
    void onCubeLoaded(CubePos pos);

    void onCubeUnloaded(CubePos pos);

    default EntitySectionStorage<Entity> sectionStorage() {
        return null;
    }
}
