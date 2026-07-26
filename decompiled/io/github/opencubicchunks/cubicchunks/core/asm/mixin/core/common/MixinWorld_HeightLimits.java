package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({World.class})
public abstract class MixinWorld_HeightLimits implements ICubicWorld {
   @Shadow
   private int field_73008_k;
   @Shadow
   @Final
   public boolean field_72995_K;
   @Shadow
   @Final
   public WorldProvider field_73011_w;
   private int updateEntities_enityChunkBlockY;

   public MixinWorld_HeightLimits() {
   }

   @Shadow
   public abstract Chunk func_175726_f(BlockPos var1);

   @Shadow
   public abstract IBlockState func_180495_p(BlockPos var1);

   @Shadow
   public abstract boolean func_175667_e(BlockPos var1);

   @Shadow
   public abstract boolean func_175668_a(BlockPos var1, boolean var2);

   @Shadow
   protected abstract boolean func_175680_a(int var1, int var2, boolean var3);

   @Overwrite
   public boolean func_189509_E(BlockPos pos) {
      return pos.func_177956_o() >= this.getMaxHeight() || pos.func_177956_o() < this.getMinHeight();
   }

   @Overwrite
   public int func_175699_k(BlockPos pos) {
      if (pos.func_177956_o() < this.getMinHeight()) {
         return 0;
      } else {
         return pos.func_177956_o() >= this.getMaxHeight() ? EnumSkyBlock.SKY.field_77198_c : this.func_175726_f(pos).func_177443_a(pos, 0);
      }
   }

   @ModifyConstant(
      method = {"getLight(Lnet/minecraft/util/math/BlockPos;Z)I"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO},
         ordinal = 0
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;getY()I"
         ),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"
         )
      )}
   )
   @Group(
      name = "getLightHeightOverride",
      max = 4
   )
   private int getLightGetYReplace(int zero) {
      return this.getMinHeight();
   }

   @ModifyConstant(
      method = {"getLight(Lnet/minecraft/util/math/BlockPos;Z)I"},
      constant = {@Constant(
         intValue = 255
      ), @Constant(
         intValue = 256
      )},
      require = 2
   )
   @Group(
      name = "getLightHeightOverride"
   )
   private int getLightGetReplacementYTooHigh(int original) {
      return this.getMaxHeight() + original - 256;
   }

   @ModifyConstant(
      method = {"getLightFor"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO}
      )}
   )
   @Group(
      name = "getLightForHeightOverride",
      min = 2,
      max = 2
   )
   private int getLightForGetMinYReplace(int origY) {
      return this.getMinHeight();
   }

   @Inject(
      method = {"isAreaLoaded(IIIIIIZ)Z"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   @Group(
      name = "isLoaded",
      max = 1
   )
   private void isAreaLoadedInject(
      int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty, @Nonnull CallbackInfoReturnable<Boolean> cbi
   ) {
      if (this.isCubicWorld()) {
         boolean ret = this.field_72995_K && allowEmpty || this.testForCubes(xStart, yStart, zStart, xEnd, yEnd, zEnd, Objects::nonNull);
         cbi.cancel();
         cbi.setReturnValue(ret);
      }
   }

   @Inject(
      method = {"isBlockLoaded(Lnet/minecraft/util/math/BlockPos;Z)Z"},
      cancellable = true,
      at = {@At("HEAD")}
   )
   public void isBlockLoaded(BlockPos pos, boolean allowEmpty, CallbackInfoReturnable<Boolean> cbi) {
      if (this.isCubicWorld()) {
         ICube cube = this.getCubeCache()
            .getLoadedCube(Coords.blockToCube(pos.func_177958_n()), Coords.blockToCube(pos.func_177956_o()), Coords.blockToCube(pos.func_177952_p()));
         if (allowEmpty) {
            cbi.setReturnValue(cube != null);
         } else {
            cbi.setReturnValue(cube != null && !(cube instanceof BlankCube));
         }
      }
   }

   @Redirect(
      method = {"spawnEntity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z"
      )
   )
   private boolean spawnEntity_isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent) {
      assert this == world;

      return this.isCubicWorld()
         ? this.func_175668_a(new BlockPos((double)Coords.cubeToMinBlock(chunkX), ent.field_70163_u, (double)Coords.cubeToMinBlock(chunkZ)), allowEmpty)
         : this.func_175680_a(chunkX, chunkZ, allowEmpty);
   }

   @Redirect(
      method = {"updateEntityWithOptionalForce"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z",
         ordinal = 0
      )
   )
   private boolean updateEntityWithOptionalForce_isChunkLoaded0(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent, boolean force) {
      assert this == world;

      return this.isCubicWorld()
         ? this.func_175668_a(new BlockPos(Coords.cubeToMinBlock(chunkX), Coords.cubeToMinBlock(ent.field_70162_ai), Coords.cubeToMinBlock(chunkZ)), allowEmpty)
         : this.func_175680_a(chunkX, chunkZ, allowEmpty);
   }

   @Redirect(
      method = {"updateEntityWithOptionalForce"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z",
         ordinal = 1
      )
   )
   private boolean updateEntityWithOptionalForce_isChunkLoaded1(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent, boolean force) {
      assert this == world;

      return this.isCubicWorld()
         ? this.func_175668_a(new BlockPos((double)Coords.cubeToMinBlock(chunkX), ent.field_70163_u, (double)Coords.cubeToMinBlock(chunkZ)), allowEmpty)
         : this.func_175680_a(chunkX, chunkZ, allowEmpty);
   }

   @Inject(
      method = {"updateEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z",
         ordinal = 0
      )},
      locals = LocalCapture.CAPTURE_FAILHARD,
      require = 1
   )
   private void updateEntities_isChunkLoaded0_getLocals(CallbackInfo cbi, int i, Entity entity, int chunkX, int chunkZ) {
      this.updateEntities_enityChunkBlockY = Coords.cubeToMinBlock(entity.field_70162_ai);
   }

   @Inject(
      method = {"updateEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z",
         ordinal = 1
      )},
      locals = LocalCapture.CAPTURE_FAILHARD,
      require = 1
   )
   private void updateEntities_isChunkLoaded1_getLocals(CallbackInfo cbi, int i, Entity entity, Entity ridingEntity, int chunkX, int chunkZ) {
      this.updateEntities_enityChunkBlockY = Coords.cubeToMinBlock(entity.field_70162_ai);
   }

   @Redirect(
      method = {"updateEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isChunkLoaded(IIZ)Z",
         ordinal = 1
      )
   )
   private boolean updateEntities_isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty) {
      assert this == world;

      return this.isCubicWorld()
         ? this.func_175668_a(new BlockPos(Coords.cubeToMinBlock(chunkX), this.updateEntities_enityChunkBlockY, Coords.cubeToMinBlock(chunkZ)), allowEmpty)
         : this.func_175680_a(chunkX, chunkZ, allowEmpty);
   }

   @Inject(
      method = {"getBiome"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getBiome(BlockPos pos, CallbackInfoReturnable<Biome> ci) {
      if (this.isCubicWorld()) {
         ICube cube = this.getCubeCache()
            .getLoadedCube(Coords.blockToCube(pos.func_177958_n()), Coords.blockToCube(pos.func_177956_o()), Coords.blockToCube(pos.func_177952_p()));
         if (cube != null) {
            Biome biome = cube.getBiome(pos);
            ci.setReturnValue(biome);
            ci.cancel();
         }
      }
   }

   @ModifyConstant(
      method = {"canSnowAtBody", "canBlockFreezeBody"},
      constant = {@Constant(
         intValue = 256
      )},
      remap = false
   )
   private int canSnowAt_getMaxHeight(int _256) {
      return this.getMaxHeight();
   }

   @ModifyConstant(
      method = {"canSnowAtBody", "canBlockFreezeBody"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      remap = false
   )
   private int canSnowAt_getMinHeight(int zero) {
      return this.getMinHeight();
   }
}
