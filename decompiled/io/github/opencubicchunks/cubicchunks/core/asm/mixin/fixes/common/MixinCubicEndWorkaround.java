package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.dragon.phase.PhaseHoldingPattern;
import net.minecraft.entity.boss.dragon.phase.PhaseLanding;
import net.minecraft.entity.boss.dragon.phase.PhaseLandingApproach;
import net.minecraft.entity.boss.dragon.phase.PhaseTakeoff;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.end.DragonFightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({EntityDragon.class, PhaseHoldingPattern.class, PhaseLanding.class, PhaseLandingApproach.class, PhaseTakeoff.class, DragonFightManager.class})
public class MixinCubicEndWorkaround {
   public MixinCubicEndWorkaround() {
   }

   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/WorldServer;getTopSolidOrLiquidBlock(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"
      ),
      require = 0
   )
   private BlockPos getTopSolidOrLiquidBlockRedirect(WorldServer world, BlockPos pos) {
      return ((ICubicWorldInternal)world).getTopSolidOrLiquidBlockVanilla(pos);
   }

   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;getTopSolidOrLiquidBlock(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"
      ),
      require = 0
   )
   private BlockPos getTopSolidOrLiquidBlockRedirect(World world, BlockPos pos) {
      return ((ICubicWorldInternal)world).getTopSolidOrLiquidBlockVanilla(pos);
   }
}
