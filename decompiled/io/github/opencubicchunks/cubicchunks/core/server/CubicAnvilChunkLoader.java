package io.github.opencubicchunks.cubicchunks.core.server;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

public class CubicAnvilChunkLoader extends AnvilChunkLoader {
   private ICubeIO cubeIOValue;
   private final Supplier<ICubeIO> cubeIOSource;

   public CubicAnvilChunkLoader(File chunkSaveLocationIn, DataFixer dataFixerIn, Supplier<ICubeIO> cubeIO) {
      super(chunkSaveLocationIn, dataFixerIn);
      this.cubeIOSource = cubeIO;
   }

   private ICubeIO getCubeIO() {
      if (this.cubeIOValue == null) {
         this.cubeIOValue = this.cubeIOSource.get();
      }

      return this.cubeIOValue;
   }

   @Nullable
   public Chunk func_75815_a(World worldIn, int x, int z) throws IOException {
      ICubeIO.PartialData<Chunk> data = ((ICubeProviderInternal.Server)worldIn.func_72863_F()).getCubeIO().loadColumnAsyncPart(worldIn, x, z);
      ((ICubeProviderInternal.Server)worldIn.func_72863_F()).getCubeIO().loadColumnSyncPart(data);
      return data.getObject();
   }

   @Nullable
   public Object[] loadChunk__Async(World worldIn, int x, int z) throws IOException {
      ICubeIO.PartialData<Chunk> data = ((ICubeProviderInternal.Server)worldIn.func_72863_F()).getCubeIO().loadColumnAsyncPart(worldIn, x, z);
      return new Object[]{data.getObject(), data.getNbt()};
   }

   public boolean func_191063_a(int x, int z) {
      return this.getCubeIO().columnExists(x, z);
   }

   @Nullable
   protected Chunk func_75822_a(World worldIn, int x, int z, NBTTagCompound compound) {
      throw new UnsupportedOperationException();
   }

   @Nullable
   protected Object[] checkedReadChunkFromNBT__Async(World worldIn, int x, int z, NBTTagCompound compound) {
      throw new UnsupportedOperationException();
   }

   public void func_75816_a(World worldIn, Chunk chunkIn) {
      this.getCubeIO().saveColumn(chunkIn);

      for (ICube cube : ((IColumn)chunkIn).getLoadedCubes()) {
         this.getCubeIO().saveCube((Cube)cube);
      }
   }

   protected void func_75824_a(ChunkPos pos, NBTTagCompound compound) {
      throw new UnsupportedOperationException();
   }

   public boolean func_75814_c() {
      return this.getCubeIO().func_75814_c();
   }

   public void func_75819_b(World worldIn, Chunk chunkIn) {
   }

   public void func_75817_a() {
   }

   public void func_75818_b() {
      try {
         this.getCubeIO().flush();
      } catch (IOException var2) {
         CubicChunks.LOGGER.catching(var2);
      }
   }

   public void loadEntities(World worldIn, NBTTagCompound compound, Chunk chunk) {
      throw new UnsupportedOperationException();
   }

   public int getPendingSaveCount() {
      return this.getCubeIO().getPendingColumnCount() + this.getCubeIO().getPendingCubeCount() / 16;
   }
}
