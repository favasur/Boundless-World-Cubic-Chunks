package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import io.github.opencubicchunks.cubicchunks.core.world.SpawnPlaceFinder;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WorldProvider.class})
public abstract class MixinWorldProvider implements ICubicWorldProvider {
   @Shadow
   protected World field_76579_a;
   @Shadow
   protected boolean field_76576_e;
   private boolean getActualHeightForceOriginalFlag = false;

   public MixinWorldProvider() {
   }

   @Shadow
   public abstract DimensionType func_186058_p();

   @Shadow
   public abstract IChunkGenerator func_186060_c();

   @Shadow(
      remap = false
   )
   public abstract int getActualHeight();

   @Overwrite(
      remap = false
   )
   public int getHeight() {
      return ((ICubicWorld)this.field_76579_a).getMaxHeight();
   }

   @Inject(
      method = {"getActualHeight"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void getActualHeight(CallbackInfoReturnable<Integer> cir) {
      if (this.field_76579_a != null && ((ICubicWorld)this.field_76579_a).isCubicWorld() && this.field_76579_a.func_175624_G() instanceof ICubicWorldType) {
         cir.setReturnValue(((ICubicWorld)this.field_76579_a).getMaxGenerationHeight());
      }
   }

   @Override
   public int getOriginalActualHeight() {
      int var1;
      try {
         this.getActualHeightForceOriginalFlag = true;
         var1 = this.getActualHeight();
      } finally {
         this.getActualHeightForceOriginalFlag = false;
      }

      return var1;
   }

   @Nullable
   @Override
   public ICubeGenerator createCubeGenerator() {
      if (!((ICubicWorld)this.field_76579_a).isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else if (this.field_76579_a.func_175624_G() instanceof ICubicWorldType
         && ((ICubicWorldType)this.field_76579_a.func_175624_G()).hasCubicGeneratorForWorld(this.field_76579_a)) {
         return ((ICubicWorldType)this.field_76579_a.func_175624_G()).createCubeGenerator(this.field_76579_a);
      } else {
         WorldSavedCubicChunksData savedData = (WorldSavedCubicChunksData)this.field_76579_a
            .getPerWorldStorage()
            .func_75742_a(WorldSavedCubicChunksData.class, "cubicChunksData");
         return ((VanillaCompatibilityGeneratorProviderBase)VanillaCompatibilityGeneratorProviderBase.REGISTRY.getValue(savedData.compatibilityGeneratorType))
            .provideGenerator(this.func_186060_c(), this.field_76579_a);
      }
   }

   @Inject(
      method = {"getRandomizedSpawnPoint"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void findRandomizedSpawnPoint(CallbackInfoReturnable<BlockPos> cir) {
      if (((ICubicWorld)this.field_76579_a).isCubicWorld()) {
         cir.setReturnValue(SpawnPlaceFinder.getRandomizedSpawnPoint(this.field_76579_a));
         cir.cancel();
      }
   }

   @Inject(
      method = {"canCoordinateBeSpawn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void canCoordinateBeSpawnCC(int x, int z, CallbackInfoReturnable<Boolean> cir) {
      if (((ICubicWorld)this.field_76579_a).isCubicWorld()) {
         cir.cancel();
         BlockPos blockpos = new BlockPos(x, 64, z);
         if (this.field_76579_a.func_180494_b(blockpos).func_185352_i()) {
            cir.setReturnValue(true);
         } else {
            BlockPos top = SpawnPlaceFinder.getTopBlockBisect(this.field_76579_a, blockpos);
            if (top == null) {
               cir.setReturnValue(false);
            } else {
               cir.setReturnValue(this.field_76579_a.func_180495_p(top).func_177230_c() == Blocks.field_150349_c);
            }
         }
      }
   }

   @Override
   public World getWorld() {
      return this.field_76579_a;
   }
}
