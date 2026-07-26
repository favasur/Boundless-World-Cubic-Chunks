package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({GuiOverlayDebug.class})
public class MixinGuiOverlayDebug {
   @Shadow
   @Final
   private Minecraft field_175242_a;

   public MixinGuiOverlayDebug() {
   }

   @ModifyConstant(
      method = {"call"},
      constant = {@Constant(
         intValue = 0,
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO}
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"
         ),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;isEmpty()Z"
         )
      )}
   )
   @Group(
      name = "getMinWorldHeight",
      min = 1,
      max = 1
   )
   private int getMinWorldHeight(int orig) {
      return ((ICubicWorld)this.field_175242_a.field_71441_e).getMinHeight();
   }

   @ModifyConstant(
      method = {"call"},
      constant = {@Constant(
         intValue = 256
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"
         ),
         to = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;isEmpty()Z"
         )
      )}
   )
   @Group(
      name = "getMaxWorldHeight",
      min = 1,
      max = 1
   )
   private int getMaxWorldHeight(int orig) {
      return ((ICubicWorld)this.field_175242_a.field_71441_e).getMaxHeight();
   }

   @Redirect(
      method = {"call"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/chunk/Chunk;getBiome(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/biome/BiomeProvider;)Lnet/minecraft/world/biome/Biome;"
      )
   )
   private Biome getBiome(Chunk chunk, BlockPos pos, BiomeProvider provider) {
      if (((ICubicWorld)chunk.func_177412_p()).isCubicWorld()) {
         ICube cube = ((IColumn)chunk).getCube(Coords.blockToCube(pos.func_177956_o()));
         return cube.getBiome(pos);
      } else {
         return chunk.func_177411_a(pos, provider);
      }
   }
}
