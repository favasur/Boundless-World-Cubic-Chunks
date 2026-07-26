package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.util.NotCubicChunksWorldException;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({World.class})
@Implements({@Interface(
      iface = ICubicWorld.class,
      prefix = "world$"
   )})
public abstract class MixinWorld implements ICubicWorldInternal {
   @Shadow
   protected IChunkProvider field_73020_y;
   @Shadow
   @Final
   @Mutable
   public WorldProvider field_73011_w;
   @Shadow
   @Final
   public Random field_73012_v;
   @Shadow
   @Final
   public boolean field_72995_K;
   @Shadow
   @Final
   public Profiler field_72984_F;
   @Shadow
   @Final
   @Mutable
   protected ISaveHandler field_73019_z;
   @Shadow
   protected boolean field_72987_B;
   @Shadow
   protected WorldInfo field_72986_A;
   @Shadow
   protected int field_73005_l;
   @Nullable
   private LightingManager lightingManager;
   protected boolean isCubicWorld;
   protected int minHeight = 0;
   protected int maxHeight = 256;
   protected int fakedMaxHeight = 0;
   private int minGenerationHeight = 0;
   private int maxGenerationHeight = 256;

   public MixinWorld() {
   }

   @Shadow
   public abstract ISaveHandler func_72860_G();

   @Shadow
   public abstract boolean func_175707_a(BlockPos var1, BlockPos var2);

   @Shadow
   public abstract boolean func_175697_a(BlockPos var1, int var2);

   @Shadow
   protected abstract boolean func_175680_a(int var1, int var2, boolean var3);

   @Shadow
   public abstract boolean func_175701_a(BlockPos var1);

   @Shadow
   public abstract GameRules func_82736_K();

   @Shadow
   public abstract boolean func_72896_J();

   @Shadow
   public abstract boolean func_72911_I();

   @Shadow
   public abstract boolean func_175727_C(BlockPos var1);

   @Shadow
   public abstract DifficultyInstance func_175649_E(BlockPos var1);

   @Shadow
   public abstract BlockPos func_175725_q(BlockPos var1);

   @Shadow
   public abstract boolean func_175711_a(StructureBoundingBox var1);

   @Shadow
   public abstract boolean func_175662_w(BlockPos var1);

   @Shadow
   public abstract boolean func_175656_a(BlockPos var1, IBlockState var2);

   @Shadow
   public abstract boolean func_175708_f(BlockPos var1, boolean var2);

   @Shadow
   public abstract boolean func_175667_e(BlockPos var1);

   @Shadow
   public abstract Biome func_180494_b(BlockPos var1);

   @Shadow
   public abstract boolean func_175668_a(BlockPos var1, boolean var2);

   @Shadow
   public abstract boolean func_189509_E(BlockPos var1);

   @Shadow
   public abstract Chunk func_175726_f(BlockPos var1);

   @Shadow
   public abstract boolean func_175678_i(BlockPos var1);

   @Shadow
   public abstract void func_175653_a(EnumSkyBlock var1, BlockPos var2, int var3);

   protected void initCubicWorld(IntRange heightRange, IntRange generationRange) {
      ((ICubicWorldSettings)this.field_72986_A).setCubic(true);
      this.minHeight = heightRange.getMin();
      this.maxHeight = heightRange.getMax();
      this.fakedMaxHeight = this.maxHeight;
      this.minGenerationHeight = generationRange.getMin();
      this.maxGenerationHeight = generationRange.getMax();
      this.lightingManager = new LightingManager((World)this);
   }

   @Override
   public boolean isCubicWorld() {
      return this.isCubicWorld;
   }

   @Override
   public int getMinHeight() {
      return this.minHeight;
   }

   @Override
   public int getMaxHeight() {
      return this.maxHeight;
   }

   @Override
   public int getMinGenerationHeight() {
      return this.minGenerationHeight;
   }

   @Override
   public int getMaxGenerationHeight() {
      return this.maxGenerationHeight;
   }

   @Override
   public ICubeProviderInternal getCubeCache() {
      if (!this.isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else {
         return (ICubeProviderInternal)this.field_73020_y;
      }
   }

   @Override
   public LightingManager getLightingManager() {
      if (!this.isCubicWorld()) {
         throw new NotCubicChunksWorldException();
      } else {
         assert this.lightingManager != null;

         return this.lightingManager;
      }
   }

   @Override
   public boolean testForCubes(CubePos start, CubePos end, Predicate<? super ICube> cubeAllowed) {
      int minCubeX = start.getX();
      int minCubeY = start.getY();
      int minCubeZ = start.getZ();
      int maxCubeX = end.getX();
      int maxCubeY = end.getY();
      int maxCubeZ = end.getZ();

      for (int cubeX = minCubeX; cubeX <= maxCubeX; cubeX++) {
         for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
            for (int cubeZ = minCubeZ; cubeZ <= maxCubeZ; cubeZ++) {
               Cube cube = this.getCubeCache().getLoadedCube(cubeX, cubeY, cubeZ);
               if (!cubeAllowed.test(cube)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   @Override
   public Cube getCubeFromCubeCoords(int cubeX, int cubeY, int cubeZ) {
      return this.getCubeCache().getCube(cubeX, cubeY, cubeZ);
   }

   @Override
   public Cube getCubeFromBlockCoords(BlockPos pos) {
      return this.getCubeFromCubeCoords(
         Coords.blockToCube(pos.func_177958_n()), Coords.blockToCube(pos.func_177956_o()), Coords.blockToCube(pos.func_177952_p())
      );
   }

   @Override
   public int getEffectiveHeight(int blockX, int blockZ) {
      return this.field_73020_y
         .func_186025_d(Coords.blockToCube(blockX), Coords.blockToCube(blockZ))
         .func_76611_b(Coords.blockToLocal(blockX), Coords.blockToLocal(blockZ));
   }

   @Override
   public void tickCubicWorld() {
      throw new NoSuchMethodError("World.tickCubicWorld: Classes extending World need to implement tickCubicWorld in CubicChunks");
   }

   @Override
   public void fakeWorldHeight(int height) {
      this.fakedMaxHeight = height;
   }

   @Overwrite
   public int func_72800_K() {
      return this.fakedMaxHeight != 0 ? this.fakedMaxHeight : this.field_73011_w.getHeight();
   }

   @Inject(
      method = {"checkLightFor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void checkLightFor(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
      if (CubicChunksConfig.fastSimplifiedSkyLight && lightType == EnumSkyBlock.SKY) {
         if (!this.func_175697_a(pos, 1)) {
            ci.setReturnValue(false);
         } else {
            int max = this.func_175678_i(pos) ? 15 : 0;
            int opacity = this.func_180495_p(pos).getLightOpacity((World)this, pos);

            for (EnumFacing value : EnumFacing.field_82609_l) {
               max = Math.max(max, (this.func_175678_i(pos.func_177972_a(value)) ? 15 : 0) - Math.max(1, opacity) * 4);
            }

            this.func_175653_a(EnumSkyBlock.SKY, pos, Math.max(7, max));
            ci.setReturnValue(true);
         }
      } else if (CubicChunksConfig.replaceLightRecheck && this.isCubicWorld()) {
         ci.setReturnValue(this.getLightingManager().checkLightFor(lightType, pos));
      }
   }

   @Inject(
      method = {"markChunkDirty"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onMarkChunkDirty(BlockPos pos, TileEntity unusedTileEntity, CallbackInfo ci) {
      if (this.isCubicWorld()) {
         Cube cube = this.getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos));
         if (cube != null) {
            cube.markDirty();
         }

         ci.cancel();
      }
   }

   @Overwrite
   public IBlockState func_180495_p(BlockPos pos) {
      if (this.func_189509_E(pos)) {
         return Blocks.field_150350_a.func_176223_P();
      } else if (this.isCubicWorld) {
         ICube cube = ((ICubeProviderInternal)this.field_73020_y)
            .getCube(Coords.blockToCube(pos.func_177958_n()), Coords.blockToCube(pos.func_177956_o()), Coords.blockToCube(pos.func_177952_p()));
         return cube.getBlockState(pos);
      } else {
         Chunk chunk = this.func_175726_f(pos);
         return chunk.func_177435_g(pos);
      }
   }

   @Inject(
      method = {"getTopSolidOrLiquidBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getTopSolidOrLiquidBlockCubicChunks(BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
      if (this.isCubicWorld()) {
         cir.cancel();
         Chunk chunk = this.func_175726_f(pos);
         BlockPos currentPos = this.func_175725_q(pos);
         int minY = currentPos.func_177956_o() - 64;

         while (currentPos.func_177956_o() >= minY) {
            BlockPos nextPos = currentPos.func_177977_b();
            IBlockState state = chunk.func_177435_g(nextPos);
            if (state.func_185904_a().func_76230_c()
               && !state.func_177230_c().isLeaves(state, (IBlockAccess)this, nextPos)
               && !state.func_177230_c().isFoliage((IBlockAccess)this, nextPos)) {
               break;
            }

            currentPos = nextPos;
         }

         cir.setReturnValue(currentPos);
      }
   }

   @Override
   public boolean isBlockColumnLoaded(BlockPos pos) {
      return this.isBlockColumnLoaded(pos, true);
   }

   @Override
   public boolean isBlockColumnLoaded(BlockPos pos, boolean allowEmpty) {
      return this.func_175680_a(Coords.blockToCube(pos.func_177958_n()), Coords.blockToCube(pos.func_177952_p()), allowEmpty);
   }
}
