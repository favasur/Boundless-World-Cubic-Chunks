package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinChunk_Column;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({EmptyChunk.class})
public abstract class MixinEmptyChunk extends MixinChunk_Column {
   private Cube blankCube;

   public MixinEmptyChunk() {
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void cubicChunkColumn_construct(World worldIn, int x, int z, CallbackInfo cbi) {
      if (((ICubicWorld)worldIn).isCubicWorld()) {
         this.blankCube = new BlankCube((Chunk)this);
      }
   }

   @Override
   public Cube getCube(int cubeY) {
      return this.blankCube;
   }

   @Override
   public Cube removeCube(int cubeY) {
      return this.blankCube;
   }

   @Override
   public void addCube(ICube cube) {
   }

   @Override
   public Collection<Cube> getLoadedCubes() {
      return Collections.emptySet();
   }

   @Override
   public Iterable<Cube> getLoadedCubes(int startY, int endY) {
      return Collections.emptySet();
   }
}
