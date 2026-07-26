package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.CubeEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IChunkProviderClient;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent.Load;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CubeProviderClient extends ChunkProviderClient implements ICubeProviderInternal {
   @Nonnull
   private ICubicWorldInternal.Client world;
   @Nonnull
   private Cube blankCube;
   @Nonnull
   private XYZMap<Cube> cubeMap = new XYZMap<>(0.7F, 8000);

   public CubeProviderClient(ICubicWorldInternal.Client world) {
      super((World)world);
      this.world = world;
      this.blankCube = new BlankCube(super.func_186025_d(Integer.MAX_VALUE, 0));
   }

   @Nullable
   @Override
   public Chunk getLoadedColumn(int x, int z) {
      return this.func_186026_b(x, z);
   }

   @Override
   public Chunk provideColumn(int x, int z) {
      return this.func_186025_d(x, z);
   }

   public Chunk func_186025_d(int x, int z) {
      return super.func_186025_d(x, z);
   }

   @Nullable
   public Chunk func_186026_b(int x, int z) {
      return super.func_186026_b(x, z);
   }

   public Chunk func_73158_c(int cubeX, int cubeZ) {
      Chunk column = new Chunk((World)this.world, cubeX, cubeZ);
      ((IChunkProviderClient)this).getLoadedChunks().put(ChunkPos.func_77272_a(cubeX, cubeZ), column);
      MinecraftForge.EVENT_BUS.post(new Load(column));
      column.func_177417_c(true);
      return column;
   }

   public boolean func_73156_b() {
      long i = System.currentTimeMillis();

      for (Cube cube : this.cubeMap) {
         cube.tickCubeCommon(() -> System.currentTimeMillis() - i > 5L);
      }

      if (System.currentTimeMillis() - i > 100L) {
         CubicChunks.LOGGER.info("Warning: Clientside chunk ticking took {} ms", System.currentTimeMillis() - i);
      }

      return false;
   }

   @Nullable
   public Cube loadCube(CubePos pos) {
      Cube cube = this.getLoadedCube(pos);
      if (cube != null) {
         return cube;
      } else {
         Chunk column = this.getLoadedColumn(pos.getX(), pos.getZ());
         if (column == null) {
            return null;
         } else {
            cube = new Cube(column, pos.getY());
            ((IColumn)column).addCube(cube);
            this.cubeMap.put(cube);
            MinecraftForge.EVENT_BUS.post(new CubeEvent.Load(cube));
            cube.setCubeLoaded();
            return cube;
         }
      }
   }

   public void unloadCube(CubePos pos) {
      Cube cube = this.getLoadedCube(pos);
      if (cube != null) {
         cube.onUnload();
         this.cubeMap.remove(pos.getX(), pos.getY(), pos.getZ());
         ((IColumn)cube.getColumn()).removeCube(pos.getY());
      }
   }

   @Override
   public Cube getCube(int cubeX, int cubeY, int cubeZ) {
      Cube cube = this.getLoadedCube(cubeX, cubeY, cubeZ);
      return cube == null ? this.blankCube : cube;
   }

   @Override
   public Cube getCube(CubePos coords) {
      return this.getCube(coords.getX(), coords.getY(), coords.getZ());
   }

   @Nullable
   @Override
   public Cube getLoadedCube(int cubeX, int cubeY, int cubeZ) {
      return this.cubeMap.get(cubeX, cubeY, cubeZ);
   }

   @Nullable
   @Override
   public Cube getLoadedCube(CubePos coords) {
      return this.getLoadedCube(coords.getX(), coords.getY(), coords.getZ());
   }

   public Iterable<Chunk> getLoadedChunks() {
      return ((IChunkProviderClient)this).getLoadedChunks().values();
   }

   public String func_73148_d() {
      return "MultiplayerChunkCache: "
         + ((IChunkProviderClient)this).getLoadedChunks().values().stream().map(c -> ((IColumn)c).getLoadedCubes().size()).reduce(Integer::sum).orElse(-1)
         + "/"
         + ((IChunkProviderClient)this).getLoadedChunks().size();
   }
}
