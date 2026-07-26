package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({TileEntityEndGateway.class})
public class MixinTileEntityEndGateway {
   public MixinTileEntityEndGateway() {
   }

   @Redirect(
      method = {"findExitPortal"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;getTopFilledSegment()I"
      )
   )
   private int getChunkTopFilledSegmentExitFromPortal(Chunk chunk) {
      int top = chunk.func_76625_h();
      return top < 0 ? 0 : top;
   }

   @Redirect(
      method = {"findSpawnpointInChunk"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;getTopFilledSegment()I"
      )
   )
   private static int getChunkTopFilledSegmentFindSpawnpoint(Chunk chunk) {
      int top = chunk.func_76625_h();
      return top < 0 ? 0 : top;
   }

   @Overwrite
   private static Chunk func_184301_a(World world, Vec3d pos) {
      Chunk chunk = world.func_72964_e(MathHelper.func_76128_c(pos.field_72450_a / 16.0), MathHelper.func_76128_c(pos.field_72449_c / 16.0));
      if (((ICubicWorld)chunk.func_177412_p()).isCubicWorld()) {
         for (int cubeY = 0; cubeY < 16; cubeY++) {
            ((IColumn)chunk).getCube(cubeY);
         }
      }

      return chunk;
   }
}
