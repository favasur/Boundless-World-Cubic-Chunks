package io.github.opencubicchunks.cubicchunks.core.asm.mixin.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Truncate path-finder traversal at extreme cubeYs so mobs on the border of
 * an unloaded cube region give up cleanly. 1.21 port: use
 * {@code PathType.WALKABLE} for the truncated path-type rather than the
 * legacy {@code PathType.OPEN} (the open/walkable enum is the same
 * semantic category post-1.20). Surface the &gt;30 000 block border as a
 * passable but unreachable cell so the navigator moves on rather than stalling.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class MixinWalkNodeProcessorHeightLimit {

    @Inject(method = "getBlockPathType(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Mob;)Lnet/minecraft/world/level/pathfinder/PathType;",
            at = @At("HEAD"), cancellable = true)
    private void cc$getBlockPathType(BlockGetter level, BlockPos pos, Mob mob, CallbackInfoReturnable<PathType> cir) {
        if (Math.abs(pos.getY()) > 30_000) {
            cir.setReturnValue(PathType.WALKABLE);
        }
    }

    @Inject(method = "getWalkableBlockPos", at = @At("HEAD"))
    private void cc$getWalkableBlockPos(BlockPos pos, Mob mob, CallbackInfoReturnable<BlockPos> cir) {
        Level mLevel = mob.level();
        if (!((ICubicWorldInternal) mLevel).isCubicWorld()) return;
        ICube cube = ((ICubicWorldInternal) mLevel).getCubeCache().getLoadedCube(
                Coords.blockToCube(pos.getX()),
                Coords.blockToCube(pos.getY()),
                Coords.blockToCube(pos.getZ()));
        if (cube == null) {
            cir.setReturnValue(null);
        }
    }
}
