package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.EntityContainer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({RenderGlobal.class})
public class MixinRenderGlobal {
   @Nullable
   private BlockPos position;
   @Shadow
   private int field_72739_F;
   @Shadow
   private ViewFrustum field_175008_n;

   public MixinRenderGlobal() {
   }

   @Inject(
      method = {"renderEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/WorldClient;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"
      )},
      locals = LocalCapture.CAPTURE_FAILSOFT
   )
   @Group(
      name = "renderEntitiesFix",
      min = 3,
      max = 3
   )
   public void onGetPosition(
      Entity renderViewEntity,
      ICamera camera,
      float partialTicks,
      CallbackInfo ci,
      int pass,
      double d0,
      double d1,
      double d2,
      Entity entity,
      double d3,
      double d4,
      double d5,
      List<Entity> list,
      List<Entity> list1,
      List<Entity> list2,
      PooledMutableBlockPos pos,
      Iterator<?> var21,
      @Coerce Object info
   ) {
      RenderChunk renderChunk = ((IContainerLocalRenderInformation)info).getRenderChunk();
      ICubicWorld world = (ICubicWorld)renderChunk.func_188283_p();
      if (world.isCubicWorld()) {
         this.position = renderChunk.func_178568_j();
      } else {
         this.position = null;
      }
   }

   @Inject(
      method = {"renderEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;",
         remap = false
      )},
      locals = LocalCapture.CAPTURE_FAILSOFT,
      remap = true
   )
   @Dynamic
   @Group(
      name = "renderEntitiesFix"
   )
   public void onGetPosition(
      Entity renderViewEntity,
      ICamera camera,
      float partialTicks,
      CallbackInfo ci,
      int pass,
      double d0,
      double d1,
      double d2,
      Entity entity,
      double d3,
      double d4,
      double d5,
      List list,
      boolean forgeEntityPass,
      boolean forgeTileEntityPass,
      boolean isShaders,
      boolean oldFancyGraphics,
      List list1,
      List list2,
      PooledMutableBlockPos pos,
      Iterator<?> var22,
      @Coerce Object info
   ) {
      RenderChunk renderChunk = ((IContainerLocalRenderInformation)info).getRenderChunk();
      ICubicWorld world = (ICubicWorld)renderChunk.func_188283_p();
      if (world.isCubicWorld()) {
         this.position = renderChunk.func_178568_j();
      } else {
         this.position = null;
      }
   }

   @Inject(
      method = {"renderEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;",
         remap = false
      )},
      locals = LocalCapture.CAPTURE_FAILSOFT,
      remap = true
   )
   @Dynamic
   @Group(
      name = "renderEntitiesFix"
   )
   public void onGetPosition(
      Entity renderViewEntity,
      ICamera camera,
      float partialTicks,
      CallbackInfo ci,
      int pass,
      double d0,
      double d1,
      double d2,
      Entity entity,
      double d3,
      double d4,
      double d5,
      List list,
      boolean forgeEntityPass,
      boolean forgeTileEntityPass,
      boolean isShaders,
      List list1,
      List list2,
      PooledMutableBlockPos pos,
      boolean playerShadowPass,
      Iterator<?> var22,
      @Coerce Object info
   ) {
      RenderChunk renderChunk = ((IContainerLocalRenderInformation)info).getRenderChunk();
      ICubicWorld world = (ICubicWorld)renderChunk.func_188283_p();
      if (world.isCubicWorld()) {
         this.position = renderChunk.func_178568_j();
      } else {
         this.position = null;
      }
   }

   @Redirect(
      method = {"renderEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/math/BlockPos;getY()I"
      ),
      require = 1
   )
   @Group(
      name = "renderEntitiesFix"
   )
   private int getRenderChunkYPos(BlockPos pos) {
      return this.position != null ? 0 : pos.func_177956_o();
   }

   @Redirect(
      method = {"renderEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;getEntityLists()[Lnet/minecraft/util/ClassInheritanceMultiMap;"
      ),
      require = 1
   )
   @Group(
      name = "renderEntitiesFix"
   )
   private ClassInheritanceMultiMap<Entity>[] getEntityList(Chunk chunk) {
      if (this.position == null) {
         return chunk.func_177429_s();
      } else {
         ICube cube = ((IColumn)chunk).getCube(Coords.blockToCube(this.position.func_177956_o()));
         return cube instanceof BlankCube ? EntityContainer.EMPTY_ARR : new ClassInheritanceMultiMap[]{cube.getEntitySet()};
      }
   }

   @ModifyConstant(
      method = {"renderWorldBorder"},
      constant = {@Constant(
         doubleValue = 0.0
      ), @Constant(
         doubleValue = 256.0
      )},
      slice = {@Slice(
         from = @At("HEAD"),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V"
         )
      )},
      require = 2
   )
   private double renderWorldBorder_getRenderHeight(double original, Entity entity, float partialTicks) {
      return original == 0.0 ? entity.field_70163_u - 128.0 : entity.field_70163_u + 128.0;
   }
}
