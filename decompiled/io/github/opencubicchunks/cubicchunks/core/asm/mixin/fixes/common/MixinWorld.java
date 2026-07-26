package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({World.class})
public abstract class MixinWorld implements ICubicWorld {
   public MixinWorld() {
   }

   @Shadow
   public abstract WorldBorder func_175723_af();

   @Shadow
   public abstract boolean func_191503_g(Entity var1);

   @Redirect(
      method = {"markAndNotifyBlock"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;isPopulated()Z"
      )
   )
   public boolean markNotifyBlock_CubeCheck(Chunk _this, BlockPos pos, Chunk chunk, IBlockState oldstate, IBlockState newState, int flags) {
      if (!this.isCubicWorld()) {
         return chunk.func_150802_k();
      } else {
         IColumn column = (IColumn)chunk;
         ICube cube = column.getCube(Coords.blockToCube(pos.func_177956_o()));
         return cube.isFullyPopulated();
      }
   }
}
