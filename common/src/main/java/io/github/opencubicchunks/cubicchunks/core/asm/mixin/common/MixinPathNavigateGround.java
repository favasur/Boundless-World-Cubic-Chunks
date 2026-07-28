package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21 port: in 1.12 {@code GroundPathNavigation.pathFinder.mob} was the
 * navigator owner; in 1.21 we cannot reach {@code PathFinder.mob} from a
 * mixin, but we can read {@code GroundPathNavigation#mob} (a protected
 * field in 1.21.x PathNavigation hierarchy) via Mixin's accessor pattern.
 * If the destination cube is not loaded for a cubic world, give up
 * cleanly so the navigator doesn't stall.
 */
@Mixin(GroundPathNavigation.class)
public abstract class MixinPathNavigateGround {

    @Inject(method = "createPathToAnyOf", at = @At("HEAD"), cancellable = true)
    private void cc$createPathToAnyOf(java.util.Set<BlockPos> targets, int range,
                                      CallbackInfoReturnable<net.minecraft.world.level.pathfinder.Path> cir) {
        if (targets == null || targets.isEmpty()) return;
        GroundPathNavigation self = (GroundPathNavigation) (Object) this;
        Mob mob = ((PathNavigationAccessor) self).cc$getMob();
        if (mob == null) return;
        Level level = mob.level();
        if (!(level instanceof ICubicWorldInternal cubic)) return;
        if (!cubic.isCubicWorld()) return;
        BlockPos target = targets.iterator().next();
        if (cubic.getCubeCache().getCube(
                Coords.blockToCube(target.getX()),
                Coords.blockToCube(target.getY()),
                Coords.blockToCube(target.getZ())) == null) {
            cir.setReturnValue(null);
        }
    }
}
