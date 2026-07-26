package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor that exposes the protected {@code mob} field on
 * {@link PathNavigation} for sibling mixins that need the navigator's owner
 * without going through {@code Mob.level()}. The 1.21.x mapping preserves the
 * field name; if future mappings relocate it, only this accessor needs an
 * update.
 */
@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
    @Accessor("mob")
    Mob cc$getMob();
}
