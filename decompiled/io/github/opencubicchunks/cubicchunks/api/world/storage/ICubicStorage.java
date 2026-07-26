package io.github.opencubicchunks.cubicchunks.api.world.storage;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

@ParametersAreNonnullByDefault
public interface ICubicStorage extends Flushable, AutoCloseable {
   boolean columnExists(ChunkPos var1) throws IOException;

   boolean cubeExists(CubePos var1) throws IOException;

   @Nonnull
   default ICubicStorage.PosBatch existsBatch(ICubicStorage.PosBatch positions) throws IOException {
      try {
         return new ICubicStorage.PosBatch(positions.columns.parallelStream().filter(pos -> {
            try {
               return this.columnExists(pos);
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         }).collect(Collectors.toSet()), positions.cubes.parallelStream().filter(pos -> {
            try {
               return this.cubeExists(pos);
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         }).collect(Collectors.toSet()));
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   NBTTagCompound readColumn(ChunkPos var1) throws IOException;

   NBTTagCompound readCube(CubePos var1) throws IOException;

   @Nonnull
   default ICubicStorage.NBTBatch readBatch(ICubicStorage.PosBatch positions) throws IOException {
      try {
         return new ICubicStorage.NBTBatch(positions.columns.parallelStream().collect(Collectors.toConcurrentMap(pos -> (ChunkPos)pos, pos -> {
            try {
               return this.readColumn(pos);
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         })), positions.cubes.parallelStream().collect(Collectors.toConcurrentMap(pos -> (CubePos)pos, pos -> {
            try {
               return this.readCube(pos);
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         })));
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   void writeColumn(ChunkPos var1, NBTTagCompound var2) throws IOException;

   void writeCube(CubePos var1, NBTTagCompound var2) throws IOException;

   default void writeBatch(ICubicStorage.NBTBatch batch) throws IOException {
      try {
         batch.columns.entrySet().parallelStream().forEach(entry -> {
            try {
               this.writeColumn(entry.getKey(), entry.getValue());
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         });
         batch.cubes.entrySet().parallelStream().forEach(entry -> {
            try {
               this.writeCube(entry.getKey(), entry.getValue());
            } catch (IOException var3x) {
               throw new UncheckedIOException(var3x);
            }
         });
      } catch (UncheckedIOException var3) {
         throw var3.getCause();
      }
   }

   void forEachColumn(Consumer<ChunkPos> var1) throws IOException;

   void forEachCube(Consumer<CubePos> var1) throws IOException;

   @Override
   void flush() throws IOException;

   @Override
   void close() throws IOException;

   public static class NBTBatch {
      public final Map<ChunkPos, NBTTagCompound> columns;
      public final Map<CubePos, NBTTagCompound> cubes;

      public NBTBatch(Map<ChunkPos, NBTTagCompound> columns, Map<CubePos, NBTTagCompound> cubes) {
         this.columns = Objects.requireNonNull(columns, "columns");
         this.cubes = Objects.requireNonNull(cubes, "cubes");
      }
   }

   public static class PosBatch {
      public final Set<ChunkPos> columns;
      public final Set<CubePos> cubes;

      public PosBatch(Set<ChunkPos> columns, Set<CubePos> cubes) {
         this.columns = Objects.requireNonNull(columns, "columns");
         this.cubes = Objects.requireNonNull(cubes, "cubes");
      }
   }
}
