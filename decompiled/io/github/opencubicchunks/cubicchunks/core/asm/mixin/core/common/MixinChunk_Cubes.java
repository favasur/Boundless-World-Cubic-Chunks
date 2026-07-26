package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import com.google.common.base.Predicate;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.StagingHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.ColumnTileEntityMap;
import io.github.opencubicchunks.cubicchunks.core.world.column.CubeMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent.Load;
import net.minecraftforge.event.world.ChunkEvent.Unload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(
   value = {Chunk.class},
   priority = 999
)
public abstract class MixinChunk_Cubes implements IColumnInternal {
   @Shadow
   @Final
   private ExtendedBlockStorage[] field_76652_q;
   @Shadow
   @Final
   public static ExtendedBlockStorage field_186036_a;
   @Shadow
   private boolean field_76644_m;
   @Shadow
   @Final
   public int field_76635_g;
   @Shadow
   @Final
   public int field_76647_h;
   @Shadow
   @Final
   private ClassInheritanceMultiMap<Entity>[] field_76645_j;
   @Shadow
   @Final
   @Mutable
   private Map<BlockPos, TileEntity> field_150816_i;
   @Shadow
   @Final
   private int[] field_76634_f;
   @Shadow
   @Final
   private World field_76637_e;
   @Shadow
   private boolean field_76636_d;
   @Shadow
   private boolean field_150815_m;
   @Shadow
   private boolean field_150814_l;
   @Shadow
   private boolean field_76643_l;
   private CubeMap cubeMap;
   private IHeightMap opacityIndex;
   private Cube cachedCube;
   private StagingHeightMap stagingHeightMap;
   private boolean isColumn = false;
   private ChunkPrimer compatGenerationPrimer;

   public MixinChunk_Cubes() {
   }

   @Shadow
   public abstract byte[] func_76605_m();

   @Shadow
   public abstract int func_76611_b(int var1, int var2);

   public <T extends World & ICubicWorldInternal> T getWorld() {
      return (T)this.field_76637_e;
   }

   @Nullable
   private ExtendedBlockStorage getEBS_CubicChunks(int index) {
      if (!this.isColumn) {
         return this.field_76652_q[index];
      } else if (this.cachedCube != null && this.cachedCube.getY() == index) {
         return this.cachedCube.getStorage();
      } else {
         Cube cube = ((ICubicWorldInternal)this.getWorld()).getCubeCache().getCube(this.field_76635_g, index, this.field_76647_h);
         if (!(cube instanceof BlankCube)) {
            this.cachedCube = cube;
         }

         return cube.getStorage();
      }
   }

   private void setEBS_CubicChunks(int index, ExtendedBlockStorage ebs) {
      if (!this.isColumn) {
         this.field_76652_q[index] = ebs;
      } else {
         if (index >= 0 && index < 16) {
            this.field_76652_q[index] = ebs;
         }

         if (this.cachedCube != null && this.cachedCube.getY() == index) {
            this.cachedCube.setStorage(ebs);
         } else {
            Cube loaded = ((ICubicWorldInternal)this.getWorld()).getCubeCache().getLoadedCube(this.field_76635_g, index, this.field_76647_h);
            if (loaded != null) {
               if (loaded.getStorage() == null) {
                  loaded.setStorage(ebs);
               } else {
                  throw new IllegalStateException(
                     String.format(
                        "Attempted to set a Cube ExtendedBlockStorage that already exists. This is not supported. CubePos(%d, %d, %d), loadedCube(%s), loadedCubeStorage(%s)",
                        this.field_76635_g,
                        index,
                        this.field_76647_h,
                        loaded,
                        loaded.getStorage()
                     )
                  );
               }
            }
         }
      }
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/World;II)V"},
      at = {@At("RETURN")}
   )
   private void cubicChunkColumn_construct(World world, int x, int z, CallbackInfo cbi) {
      if (world != null) {
         if (((ICubicWorld)world).isCubicWorld()) {
            this.isColumn = true;
            this.cubeMap = new CubeMap();
            if (world.field_72995_K) {
               this.opacityIndex = new ClientHeightMap((Chunk)this, this.field_76634_f);
            } else {
               this.opacityIndex = new ServerHeightMap(this.field_76634_f);
            }

            this.stagingHeightMap = new StagingHeightMap();
            this.field_150816_i = new ColumnTileEntityMap(this);
            Arrays.fill(this.func_76605_m(), (byte)-1);
         }
      }
   }

   @ModifyConstant(
      method = {"<init>(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V"},
      constant = {@Constant(
         intValue = 16,
         ordinal = 0
      )},
      require = 1
   )
   private int getInitChunkLoopEnd(int _16, World world, ChunkPrimer primer, int x, int z) {
      if (((ICubicWorldInternal.Server)world).isCompatGenerationScope()) {
         this.compatGenerationPrimer = primer;
         return -1;
      } else {
         return _16;
      }
   }

   @Override
   public ChunkPrimer getCompatGenerationPrimer() {
      return this.compatGenerationPrimer;
   }

   @Inject(
      method = {"getTopFilledSegment"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getTopFilledSegment_CubicChunks(CallbackInfoReturnable<Integer> cbi) {
      if (this.isColumn) {
         int blockY = -2147483616;

         for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
               int y = this.opacityIndex.getTopBlockY(localX, localZ);
               if (y > blockY) {
                  blockY = y;
               }
            }
         }

         if (blockY < ((IMinMaxHeight)this.getWorld()).getMinHeight()) {
            int ret = Coords.cubeToMinBlock(Coords.blockToCube(this.getWorld().field_73011_w.func_76557_i()));
            cbi.setReturnValue(ret);
            cbi.cancel();
         } else {
            int ret = Coords.cubeToMinBlock(Coords.blockToCube(blockY));
            cbi.setReturnValue(ret);
            cbi.cancel();
         }
      }
   }

   @Inject(
      method = {"generateSkylightMap"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void generateSkylightMap_CubicChunks_Replace(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Inject(
      method = {"propagateSkylightOcclusion"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void propagateSkylightOcclusion_CubicChunks_Replace(int x, int z, CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Inject(
      method = {"recheckGaps"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void recheckGaps_CubicChunks_Replace(boolean p_150803_1_, CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;relightBlock(III)V"
      )},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void setBlockState_CubicChunks_relightBlockReplace(
      BlockPos pos,
      IBlockState state,
      CallbackInfoReturnable<IBlockState> cir,
      int localX,
      int y,
      int localZ,
      int packedXZ,
      int oldHeightValue,
      IBlockState oldState,
      Block newBlock,
      Block oldBlock,
      int oldOpacity,
      ExtendedBlockStorage ebs,
      boolean createdNewEbsAboveTop,
      int newOpacity
   ) {
      if (this.isColumn && this.getCube(Coords.blockToCube(y)).isInitialLightingDone()) {
         if (oldHeightValue == y + 1) {
            ((ICubicWorldInternal)this.getWorld())
               .getLightingManager()
               .doOnBlockSetLightUpdates((Chunk)this, localX, this.func_76611_b(localX, localZ), y, localZ);
         } else {
            ((ICubicWorldInternal)this.getWorld()).getLightingManager().doOnBlockSetLightUpdates((Chunk)this, localX, oldHeightValue, y, localZ);
         }
      }
   }

   @Inject(
      method = {"relightBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void relightBlock_CubicChunks_Replace(int x, int y, int z, CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Redirect(
      method = {"getBlockLightOpacity(III)I"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"
      )
   )
   private boolean getBlockLightOpacity_isChunkLoadedCubeRedirect(Chunk chunk, int x, int y, int z) {
      if (!this.isColumn) {
         return this.field_76636_d;
      } else {
         ICube cube = this.getLoadedCube(Coords.blockToCube(y));
         return cube != null && cube.isCubeLoaded();
      }
   }

   @ModifyConstant(
      method = {"getBlockState(III)Lnet/minecraft/block/state/IBlockState;"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      require = 1
   )
   private int getBlockState_getMinHeight(int zero) {
      return this.isColumn ? Integer.MIN_VALUE : 0;
   }

   @Redirect(
      method = {"getBlockState(III)Lnet/minecraft/block/state/IBlockState;"},
      at = @At(
         value = "FIELD",
         args = {"array=length"},
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;"
      )
   )
   private int getBlockState_getMaxHeight(ExtendedBlockStorage[] ebs) {
      return this.isColumn ? Integer.MAX_VALUE : ebs.length;
   }

   @Redirect(
      method = {"getBlockState(III)Lnet/minecraft/block/state/IBlockState;"},
      at = @At(
         value = "FIELD",
         args = {"array=get"},
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;"
      )
   )
   private ExtendedBlockStorage getBlockState_getMaxHeight(ExtendedBlockStorage[] ebs, int y) {
      return this.getEBS_CubicChunks(y);
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;set(IIILnet/minecraft/block/state/IBlockState;)V",
         shift = At.Shift.AFTER
      )}
   )
   private void onEBSSet_setBlockState_setOpacity(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
      if (this.isColumn) {
         if (this.getCube(Coords.blockToCube(pos.func_177956_o())).isSurfaceTracked()) {
            this.opacityIndex
               .onOpacityChange(
                  Coords.blockToLocal(pos.func_177958_n()),
                  pos.func_177956_o(),
                  Coords.blockToLocal(pos.func_177952_p()),
                  state.getLightOpacity(this.field_76637_e, pos)
               );
            ((ICubicWorldInternal)this.getWorld()).getLightingManager().sendHeightMapUpdate(pos);
         } else {
            this.stagingHeightMap
               .onOpacityChange(
                  Coords.blockToLocal(pos.func_177958_n()),
                  pos.func_177956_o(),
                  Coords.blockToLocal(pos.func_177952_p()),
                  state.getLightOpacity(this.field_76637_e, pos)
               );
         }
      }
   }

   @Redirect(
      method = {"setBlockState"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=get"}
      )
   )
   private ExtendedBlockStorage setBlockState_CubicChunks_EBSGetRedirect(ExtendedBlockStorage[] array, int index) {
      return this.getEBS_CubicChunks(index);
   }

   @Redirect(
      method = {"setBlockState"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=set"}
      )
   )
   private void setBlockState_CubicChunks_EBSSetRedirect(ExtendedBlockStorage[] array, int index, ExtendedBlockStorage val) {
      this.setEBS_CubicChunks(index, val);
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=set"}
      )},
      cancellable = true
   )
   private void setBlockState_CubicChunks_EBSSetInject(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
      if (this.isColumn && ((ICubicWorldInternal)this.getWorld()).getCubeCache().getLoadedCube(CubePos.fromBlockCoords(pos)) == null) {
         cir.setReturnValue(null);
         cir.cancel();
      }
   }

   @Redirect(
      method = {"setBlockState"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;dirty:Z"
      )
   )
   private void setIsModifiedFromSetBlockState_Field(Chunk chunk, boolean isModifiedIn, BlockPos pos, IBlockState state) {
      if (this.isColumn) {
         ((ICubicWorldInternal)this.getWorld()).getCubeFromBlockCoords(pos).markDirty();
      } else {
         this.field_76643_l = isModifiedIn;
      }
   }

   @Redirect(
      method = {"getLightFor"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=get"}
      )
   )
   private ExtendedBlockStorage getLightFor_CubicChunks_EBSGetRedirect(ExtendedBlockStorage[] array, int index) {
      return this.getEBS_CubicChunks(index);
   }

   @Redirect(
      method = {"setLightFor"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=get"}
      )
   )
   private ExtendedBlockStorage setLightFor_CubicChunks_EBSGetRedirect(ExtendedBlockStorage[] array, int index) {
      return this.getEBS_CubicChunks(index);
   }

   @Redirect(
      method = {"setLightFor"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=set"}
      )
   )
   private void setLightFor_CubicChunks_EBSSetRedirect(ExtendedBlockStorage[] array, int index, ExtendedBlockStorage ebs) {
      this.setEBS_CubicChunks(index, ebs);
   }

   @Redirect(
      method = {"setLightFor"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;dirty:Z"
      )
   )
   private void setIsModifiedFromSetLightFor_Field(Chunk chunk, boolean isModifiedIn, EnumSkyBlock type, BlockPos pos, int value) {
      if (this.isColumn) {
         ((ICubicWorldInternal)this.getWorld()).getCubeFromBlockCoords(pos).markDirty();
      } else {
         this.field_76643_l = isModifiedIn;
      }
   }

   @Redirect(
      method = {"getLightSubtracted"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;storageArrays:[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
         args = {"array=get"}
      )
   )
   private ExtendedBlockStorage getLightSubtracted_CubicChunks_EBSGetRedirect(ExtendedBlockStorage[] array, int index) {
      return this.getEBS_CubicChunks(index);
   }

   @ModifyConstant(
      method = {"addEntity"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO},
         intValue = 0
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE:LAST",
            target = "Lnet/minecraft/util/math/MathHelper;floor(D)I"
         ),
         to = @At(
            value = "FIELD:FIRST",
            target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;"
         )
      )},
      require = 1
   )
   private int addEntity_getMinY(int zero) {
      return Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight());
   }

   @Redirect(
      method = {"addEntity"},
      at = @At(
         value = "FIELD",
         args = {"array=length"},
         target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;"
      ),
      require = 2
   )
   private int addEntity_getMaxHeight(ClassInheritanceMultiMap<?>[] entityLists) {
      return this.isColumn ? Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight()) : entityLists.length;
   }

   @Redirect(
      method = {"addEntity"},
      at = @At(
         value = "FIELD",
         args = {"array=get"},
         target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;"
      ),
      require = 1
   )
   private ClassInheritanceMultiMap<?> addEntity_getEntityList(ClassInheritanceMultiMap<?>[] entityLists, int idx, Entity entity) {
      if (!this.isColumn) {
         return entityLists[idx];
      } else if (this.cachedCube != null && this.cachedCube.getY() == idx) {
         this.cachedCube.getEntityContainer().addEntity(entity);
         return null;
      } else {
         ((ICubicWorldInternal)this.getWorld()).getCubeCache().getCube(this.field_76635_g, idx, this.field_76647_h).getEntityContainer().addEntity(entity);
         return null;
      }
   }

   @Redirect(
      method = {"addEntity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/ClassInheritanceMultiMap;add(Ljava/lang/Object;)Z"
      ),
      require = 1
   )
   private boolean addEntity_getEntityList(ClassInheritanceMultiMap<Object> obj, Object entity) {
      if (!this.isColumn) {
         return obj.add(entity);
      } else {
         assert obj == null;

         return true;
      }
   }

   @ModifyConstant(
      method = {"removeEntityAtIndex"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.LESS_THAN_ZERO},
         intValue = 0
      )},
      require = 2,
      slice = {@Slice(
         from = @At("HEAD"),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ClassInheritanceMultiMap;remove(Ljava/lang/Object;)Z"
         )
      )}
   )
   private int removeEntityAtIndex_getMinY(int zero) {
      return Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight());
   }

   @Redirect(
      method = {"removeEntityAtIndex"},
      at = @At(
         value = "FIELD",
         args = {"array=length"},
         target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;"
      ),
      require = 2
   )
   private int removeEntityAtIndex_getMaxHeight(ClassInheritanceMultiMap<?>[] entityLists) {
      return this.isColumn ? Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight()) : entityLists.length;
   }

   @Redirect(
      method = {"removeEntityAtIndex"},
      at = @At(
         value = "FIELD",
         args = {"array=get"},
         target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;"
      ),
      require = 1
   )
   private ClassInheritanceMultiMap<?> removeEntityAtIndex_getEntityList(ClassInheritanceMultiMap<?>[] entityLists, int idx, Entity entity, int index) {
      if (!this.isColumn) {
         return entityLists[idx];
      } else if (this.cachedCube != null && this.cachedCube.getY() == idx) {
         this.cachedCube.getEntityContainer().remove(entity);
         return null;
      } else {
         ((ICubicWorldInternal)this.getWorld()).getCubeCache().getCube(this.field_76635_g, idx, this.field_76647_h).getEntityContainer().remove(entity);
         return null;
      }
   }

   @Redirect(
      method = {"removeEntityAtIndex"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/ClassInheritanceMultiMap;remove(Ljava/lang/Object;)Z"
      ),
      require = 1
   )
   private boolean removeEntityAtIndex_getEntityList(ClassInheritanceMultiMap<Object> obj, Object entity) {
      if (!this.isColumn) {
         return obj.remove(entity);
      } else {
         assert obj == null;

         return true;
      }
   }

   @Redirect(
      method = {"addTileEntity(Lnet/minecraft/tileentity/TileEntity;)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"
      )
   )
   private boolean addTileEntity_isChunkLoadedCubeRedirect(Chunk chunk, TileEntity te) {
      if (!this.isColumn) {
         return this.field_76636_d;
      } else {
         ICube cube = this.getLoadedCube(Coords.blockToCube(te.func_174877_v().func_177956_o()));
         return cube != null && cube.isCubeLoaded();
      }
   }

   @Redirect(
      method = {"removeTileEntity"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"
      )
   )
   private boolean removeTileEntity_isChunkLoadedCubeRedirect(Chunk chunk, BlockPos pos) {
      if (!this.isColumn) {
         return this.field_76636_d;
      } else {
         ICube cube = this.getLoadedCube(Coords.blockToCube(pos.func_177956_o()));
         return cube != null && cube.isCubeLoaded();
      }
   }

   @Inject(
      method = {"onLoad"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onChunkLoad_CubicChunks(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
         this.field_76636_d = true;

         for (Cube cube : this.cubeMap) {
            cube.onLoad();
         }

         MinecraftForge.EVENT_BUS.post(new Load((Chunk)this));
      }
   }

   @Inject(
      method = {"onUnload"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onChunkUnload_CubicChunks(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
         this.field_76636_d = false;

         for (Cube cube : this.cubeMap) {
            cube.onUnload();
         }

         MinecraftForge.EVENT_BUS.post(new Unload((Chunk)this));
      }
   }

   @Inject(
      method = {"getEntitiesWithinAABBForEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getEntitiesWithinAABBForEntity_CubicChunks(
      @Nullable Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate<? super Entity> filter, CallbackInfo cbi
   ) {
      if (this.isColumn) {
         cbi.cancel();
         int minY = MathHelper.func_76128_c((aabb.field_72338_b - World.MAX_ENTITY_RADIUS) / 16.0);
         int maxY = MathHelper.func_76128_c((aabb.field_72337_e + World.MAX_ENTITY_RADIUS) / 16.0);
         minY = MathHelper.func_76125_a(
            minY, Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight()), Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight())
         );
         maxY = MathHelper.func_76125_a(
            maxY, Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight()), Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight())
         );

         for (Cube cube : this.cubeMap.cubes(minY, maxY)) {
            if (!cube.getEntityContainer().getEntitySet().isEmpty()) {
               for (Entity entity : cube.getEntityContainer().getEntitySet()) {
                  if (entity.func_174813_aQ().func_72326_a(aabb) && entity != entityIn) {
                     if (filter == null || filter.apply(entity)) {
                        listToFill.add(entity);
                     }

                     Entity[] parts = entity.func_70021_al();
                     if (parts != null) {
                        for (Entity part : parts) {
                           if (part != entityIn && part.func_174813_aQ().func_72326_a(aabb) && (filter == null || filter.apply(part))) {
                              listToFill.add(part);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"getEntitiesOfTypeWithinAABB"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public <T extends Entity> void getEntitiesOfTypeWithinAAAB_CubicChunks(
      Class<? extends T> entityClass, AxisAlignedBB aabb, List<T> listToFill, Predicate<? super T> filter, CallbackInfo cbi
   ) {
      if (this.isColumn) {
         cbi.cancel();
         int minY = MathHelper.func_76128_c((aabb.field_72338_b - World.MAX_ENTITY_RADIUS) / 16.0);
         int maxY = MathHelper.func_76128_c((aabb.field_72337_e + World.MAX_ENTITY_RADIUS) / 16.0);
         minY = MathHelper.func_76125_a(
            minY, Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight()), Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight())
         );
         maxY = MathHelper.func_76125_a(
            maxY, Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMinHeight()), Coords.blockToCube(((IMinMaxHeight)this.getWorld()).getMaxHeight())
         );

         for (Cube cube : this.cubeMap.cubes(minY, maxY)) {
            for (T t : cube.getEntityContainer().getEntitySet().func_180215_b(entityClass)) {
               if (t.func_174813_aQ().func_72326_a(aabb) && (filter == null || filter.apply(t))) {
                  listToFill.add(t);
               }
            }
         }
      }
   }

   @Inject(
      method = {"getPrecipitationHeight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getPrecipitationHeight_CubicChunks_Replace(BlockPos pos, CallbackInfoReturnable<BlockPos> cbi) {
      if (this.isColumn) {
         BlockPos ret = new BlockPos(
            pos.func_177958_n(),
            this.getHeightValue(Coords.blockToLocal(pos.func_177958_n()), pos.func_177956_o(), Coords.blockToLocal(pos.func_177952_p())),
            pos.func_177952_p()
         );
         cbi.setReturnValue(ret);
         cbi.cancel();
      }
   }

   @Inject(
      method = {"onTick"},
      at = {@At("RETURN")}
   )
   private void onTick_CubicChunks_TickCubes(boolean tryToTickFaster, CallbackInfo cbi) {
      if (this.isColumn) {
         this.field_150815_m = true;
         this.field_150814_l = true;
      }
   }

   @Overwrite
   public boolean func_76606_c(int startY, int endY) {
      if (startY < ((IMinMaxHeight)this.getWorld()).getMinHeight()) {
         startY = ((IMinMaxHeight)this.getWorld()).getMinHeight();
      }

      if (endY >= ((IMinMaxHeight)this.getWorld()).getMaxHeight()) {
         endY = ((IMinMaxHeight)this.getWorld()).getMaxHeight() - 1;
      }

      for (int i = startY; i <= endY; i += 16) {
         ExtendedBlockStorage extendedblockstorage = this.getEBS_CubicChunks(Coords.blockToCube(i));
         if (extendedblockstorage != field_186036_a && !extendedblockstorage.func_76663_a()) {
            return false;
         }
      }

      return true;
   }

   @Inject(
      method = {"setStorageArrays"},
      at = {@At("HEAD")}
   )
   private void setStorageArrays_CubicChunks_NotSupported(ExtendedBlockStorage[] newStorageArrays, CallbackInfo cbi) {
      if (this.isColumn) {
         throw new UnsupportedOperationException("setting storage arrays it not supported with cubic chunks");
      }
   }

   @Inject(
      method = {"checkLight()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void checkLight_CubicChunks_NotSupported(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
      }
   }

   @Redirect(
      method = {"removeInvalidTileEntity"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"
      )
   )
   private boolean removeInvalidTileEntity_isChunkLoadedCubeRedirect(Chunk chunk, BlockPos pos) {
      if (!this.isColumn) {
         return this.field_76636_d;
      } else {
         ICube cube = this.getLoadedCube(Coords.blockToCube(pos.func_177956_o()));
         return cube != null && cube.isCubeLoaded();
      }
   }

   @Inject(
      method = {"enqueueRelightChecks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void enqueueRelightChecks_CubicChunks(CallbackInfo cbi) {
      if (this.isColumn) {
         cbi.cancel();
         if (!this.field_76637_e.field_72995_K || CubicChunksConfig.doClientLightFixes) {
            this.cubeMap.enqueueRelightChecks();
         }
      }
   }
}
