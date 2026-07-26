package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({PathNavigate.class})
public abstract class MixinPathNavigate {
   @Shadow
   protected EntityLiving field_75515_a;
   @Shadow
   protected NodeProcessor field_179695_a;
   @Shadow
   protected Path field_75514_c;
   @Shadow
   protected World field_75513_b;

   public MixinPathNavigate() {
   }

   @Shadow
   protected abstract Vec3d func_75502_i();

   @Redirect(
      method = {"getPathToPos"},
      at = @At(
         value = "NEW",
         target = "net/minecraft/world/ChunkCache"
      )
   )
   private ChunkCache newChunkCacheToPosRedirect(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, BlockPos target) {
      int x1 = (int)this.field_75515_a.field_70165_t;
      int y1 = (int)this.field_75515_a.field_70163_u;
      int z1 = (int)this.field_75515_a.field_70161_v;
      int x2 = target.func_177958_n();
      int y2 = target.func_177956_o();
      int z2 = target.func_177952_p();
      return new ChunkCache(
         worldIn, new BlockPos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)), new BlockPos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)), 4
      );
   }

   @Redirect(
      method = {"getPathToEntityLiving"},
      at = @At(
         value = "NEW",
         target = "net/minecraft/world/ChunkCache"
      )
   )
   private ChunkCache newChunkCacheToLivingRedirect(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, Entity target) {
      int x1 = (int)this.field_75515_a.field_70165_t;
      int y1 = (int)this.field_75515_a.field_70163_u;
      int z1 = (int)this.field_75515_a.field_70161_v;
      int x2 = (int)target.field_70165_t;
      int y2 = (int)target.field_70163_u;
      int z2 = (int)target.field_70161_v;
      return new ChunkCache(
         worldIn, new BlockPos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)), new BlockPos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)), 4
      );
   }

   @Inject(
      method = {"pathFollow"},
      at = {@At("HEAD")}
   )
   private void pathFollowInitWalkNodeProcessor(CallbackInfo ci) {
      Vec3d vec1 = this.func_75502_i();
      Vec3d vec2 = this.field_75514_c.func_75881_a(this.field_75515_a, this.field_75514_c.func_75874_d() - 1);
      int x1 = (int)vec1.field_72450_a;
      int y1 = (int)vec1.field_72448_b;
      int z1 = (int)vec1.field_72449_c;
      int x2 = (int)vec2.field_72450_a;
      int y2 = (int)vec2.field_72448_b;
      int z2 = (int)vec2.field_72449_c;
      int maxChacheSize = 256;
      if (x2 - x1 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache X size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         x2 = x1 + maxChacheSize;
      } else if (x1 - x2 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache X size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         x2 = x1 - maxChacheSize;
      }

      if (z2 - z1 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache Z size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         z2 = z1 + maxChacheSize;
      } else if (z1 - z2 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache Z size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         z2 = z1 - maxChacheSize;
      }

      if (y2 - y1 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache Y size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         y2 = y1 + maxChacheSize;
      } else if (y1 - y2 > maxChacheSize) {
         CubicChunks.LOGGER.warn("ChunkCache Y size requested by WalkNodeProcessor is too big! Capped to " + maxChacheSize);
         y2 = y1 - maxChacheSize;
      }

      ChunkCache chunkCache = new ChunkCache(
         this.field_75513_b,
         new BlockPos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
         new BlockPos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)),
         4
      );
      this.field_179695_a.func_186315_a(chunkCache, this.field_75515_a);
   }

   @Inject(
      method = {"pathFollow"},
      at = {@At("RETURN")}
   )
   private void pathFollowShutWalkNodeProcessor(CallbackInfo ci) {
      this.field_179695_a.func_176163_a();
   }
}
