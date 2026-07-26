package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({PlayerList.class})
public abstract class MixinPlayerList implements ICubicPlayerList {
   @Shadow
   private int field_72402_d;
   @Shadow
   @Final
   private MinecraftServer field_72400_f;
   protected int verticalViewDistance = -1;

   public MixinPlayerList() {
   }

   @Redirect(
      method = {"playerLoggedOut"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;markDirty()V",
         ordinal = 0
      ),
      require = 1
   )
   private void setChunkModifiedOnPlayerLoggedOut(Chunk chunkIn, EntityPlayerMP playerIn) {
      ICubicWorldInternal world = (ICubicWorldInternal)playerIn.func_71121_q();
      if (world.isCubicWorld()) {
         world.getCubeFromCubeCoords(playerIn.field_70176_ah, playerIn.field_70162_ai, playerIn.field_70164_aj).markDirty();
      } else {
         ((World)world).func_72964_e(playerIn.field_70176_ah, playerIn.field_70164_aj).func_76630_e();
      }
   }

   @Override
   public int getVerticalViewDistance() {
      return this.verticalViewDistance < 0 ? this.field_72402_d : this.verticalViewDistance;
   }

   @Override
   public int getRawVerticalViewDistance() {
      return this.verticalViewDistance;
   }

   @Override
   public void setVerticalViewDistance(int dist) {
      this.verticalViewDistance = dist;
      if (this.field_72400_f.field_71305_c != null) {
         for (WorldServer worldserver : this.field_72400_f.field_71305_c) {
            if (worldserver != null && ((ICubicWorld)worldserver).isCubicWorld()) {
               ((PlayerCubeMap)worldserver.func_184164_w()).setPlayerViewDistance(this.field_72402_d, dist);
               ((ICubicEntityTracker)worldserver.func_73039_n()).setVertViewDistance(dist);
            }
         }
      }
   }

   @Inject(
      method = {"recreatePlayerEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/gen/ChunkProviderServer;provideChunk(II)Lnet/minecraft/world/chunk/Chunk;"
      )}
   )
   private void createPlayerChunk(EntityPlayerMP playerIn, int dimension, boolean conqueredEnd, CallbackInfoReturnable<EntityPlayerMP> cir) {
      if (((ICubicWorld)playerIn.field_70170_p).isCubicWorld()) {
         for (int dCubeY = -8; dCubeY <= 8; dCubeY++) {
            ((ICubicWorld)playerIn.field_70170_p).getCubeFromBlockCoords(playerIn.func_180425_c().func_177981_b(Coords.cubeToMinBlock(dCubeY)));
         }
      }
   }

   @ModifyConstant(
      method = {"recreatePlayerEntity"},
      constant = {@Constant(
         doubleValue = 256.0
      )}
   )
   private double getMaxHeight(double _256, EntityPlayerMP playerIn, int dimension, boolean conqueredEnd) {
      return !playerIn.field_70170_p.func_175667_e(new BlockPos(playerIn))
         ? Double.NEGATIVE_INFINITY
         : (double)((ICubicWorld)playerIn.field_70170_p).getMaxHeight();
   }
}
