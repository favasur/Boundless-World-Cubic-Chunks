package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature.ICubicFeatureStart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({StructureStart.class})
public abstract class MixinStructureStart implements ICubicFeatureStart {
   private int cubeY;
   private boolean isCubic = false;

   public MixinStructureStart() {
   }

   @Shadow
   public abstract int func_143019_e();

   @Shadow
   public abstract int func_143018_f();

   @Override
   public int getChunkPosY() {
      return this.cubeY;
   }

   @Override
   public void initCubic(World world, int cubeY) {
      if (this.isCubic) {
         throw new IllegalStateException("Already initialized!");
      } else {
         this.cubeY = cubeY;
         this.isCubic = true;
      }
   }

   @Override
   public CubePos getCubePos() {
      return new CubePos(this.getX(), this.getY(), this.getZ());
   }

   @Inject(
      method = {"writeStructureComponentsToNBT"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/nbt/NBTTagCompound;setInteger(Ljava/lang/String;I)V",
         ordinal = 0
      )},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void writeYToNbt(int chunkX, int chunkZ, CallbackInfoReturnable<NBTTagCompound> cir, NBTTagCompound tag) {
      tag.func_74768_a("ChunkY", this.cubeY);
   }

   @Inject(
      method = {"readStructureComponentsFromNBT"},
      at = {@At("HEAD")}
   )
   private void readYFromNBT(World world, NBTTagCompound tag, CallbackInfo cbi) {
      if (tag.func_74764_b("ChunkY")) {
         this.isCubic = true;
         this.cubeY = tag.func_74762_e("ChunkY");
      }
   }

   @Override
   public int getX() {
      return this.func_143019_e();
   }

   @Override
   public int getY() {
      return this.getChunkPosY();
   }

   @Override
   public int getZ() {
      return this.func_143018_f();
   }

   @Override
   public boolean isCubic() {
      return this.isCubic;
   }
}
