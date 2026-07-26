package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"net.minecraft.block.BlockBeacon$1"}
)
public class MixinBlockBeaconAsyncUpdate {
   @Shadow(
      remap = false,
      aliases = {"field_180358_a"}
   )
   @Final
   World val$worldIn;
   @Shadow(
      remap = false,
      aliases = {"field_180357_b"}
   )
   @Final
   BlockPos val$glassPos;

   public MixinBlockBeaconAsyncUpdate() {
   }

   @Inject(
      method = {"run"},
      at = {@At("HEAD")}
   )
   private void runCubicChunks(CallbackInfo ci) {
      if (((ICubicWorld)this.val$worldIn).isCubicWorld()) {
         ci.cancel();
         int blockX = this.val$glassPos.func_177958_n();
         int blockZ = this.val$glassPos.func_177952_p();
         int blockY = this.val$glassPos.func_177956_o();
         int cubeX = Coords.blockToCube(blockX);
         int cubeZ = Coords.blockToCube(blockZ);
         int cubeY = Coords.blockToCube(this.val$glassPos.func_177956_o());
         ICubeProviderServer cubeProvider = ((ICubicWorldServer)this.val$worldIn).getCubeCache();
         ICube cube = cubeProvider.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.GET_CACHED);

         while (cube != null) {
            BlockPos blockpos = new BlockPos(blockX, blockY, blockZ);
            if (!cube.getColumn().func_177444_d(blockpos)) {
               break;
            }

            IBlockState block = cube.getBlockState(blockpos);
            if (block.func_177230_c() == Blocks.field_150461_bJ) {
               ((WorldServer)this.val$worldIn).func_152344_a(() -> {
                  TileEntity tileentity = this.val$worldIn.func_175625_s(blockpos);
                  if (tileentity instanceof TileEntityBeacon) {
                     ((TileEntityBeacon)tileentity).func_174908_m();
                     this.val$worldIn.func_175641_c(blockpos, Blocks.field_150461_bJ, 1, 0);
                  }
               });
            }

            cubeY = Coords.blockToCube(--blockY);
            cube = cubeProvider.getCube(cubeX, cubeY, cubeZ, ICubeProviderServer.Requirement.GET_CACHED);
         }
      }
   }
}
