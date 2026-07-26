package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.region.ShadowPagingRegion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCounted;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

public class RegionCubeStorage implements ICubicStorage {
   private final Path path;
   private SaveCubeColumns save;

   private static SaveCubeColumns saveForPath(Path path) throws IOException {
      if (CubicChunksConfig.useShadowPagingIO) {
         Utils.createDirectories(path);
         Path part2d = path.resolve("region2d");
         Utils.createDirectories(part2d);
         Path part3d = path.resolve("region3d");
         Utils.createDirectories(part3d);
         SaveSection2D section2d = new SaveSection2D(
            new SharedCachedRegionProvider<>(
               new SimpleRegionProvider<>(
                  new EntryLocation2D.Provider(),
                  part2d,
                  (keyProv, r) -> ShadowPagingRegion.<EntryLocation2D>builder()
                        .setDirectory(part2d)
                        .setRegionKey(r)
                        .setKeyProvider(keyProv)
                        .setSectorSize(512)
                        .build(),
                  (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
               )
            ),
            new SharedCachedRegionProvider<>(
               new SimpleRegionProvider<>(
                  new EntryLocation2D.Provider(),
                  part2d,
                  (keyProvider, regionKey) -> new ExtRegion<>(part2d, Collections.emptyList(), keyProvider, regionKey),
                  (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
               )
            )
         );
         SaveSection3D section3d = new SaveSection3D(
            new SharedCachedRegionProvider<>(
               new SimpleRegionProvider<>(
                  new EntryLocation3D.Provider(),
                  part3d,
                  (keyProv, r) -> ShadowPagingRegion.<EntryLocation3D>builder()
                        .setDirectory(part3d)
                        .setRegionKey(r)
                        .setKeyProvider(keyProv)
                        .setSectorSize(512)
                        .build(),
                  (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
               )
            ),
            new SharedCachedRegionProvider<>(
               new SimpleRegionProvider<>(
                  new EntryLocation3D.Provider(),
                  part3d,
                  (keyProvider, regionKey) -> new ExtRegion<>(part3d, Collections.emptyList(), keyProvider, regionKey),
                  (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
               )
            )
         );
         return new SaveCubeColumns(section2d, section3d);
      } else {
         return SaveCubeColumns.create(path);
      }
   }

   public RegionCubeStorage(Path path) throws IOException {
      this.path = Objects.requireNonNull(path, "path");
      this.save = saveForPath(path);
   }

   @Override
   public boolean columnExists(ChunkPos pos) throws IOException {
      return this.save.getSaveSection2D().hasEntry(new EntryLocation2D(pos.field_77276_a, pos.field_77275_b));
   }

   @Override
   public boolean cubeExists(CubePos pos) throws IOException {
      return this.save.getSaveSection3D().hasEntry(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()));
   }

   @Override
   public NBTTagCompound readColumn(ChunkPos pos) throws IOException {
      Optional<ByteBuffer> data = this.save.load(new EntryLocation2D(pos.field_77276_a, pos.field_77275_b), true);
      return data.isPresent() ? CompressedStreamTools.func_74796_a(new ByteArrayInputStream(data.get().array())) : null;
   }

   @Override
   public NBTTagCompound readCube(CubePos pos) throws IOException {
      Optional<ByteBuffer> data = this.save.load(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()), true);
      return data.isPresent() ? CompressedStreamTools.func_74796_a(new ByteArrayInputStream(data.get().array())) : null;
   }

   @Override
   public void writeColumn(ChunkPos pos, NBTTagCompound nbt) throws IOException {
      ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();

      try {
         CompressedStreamTools.func_74799_a(nbt, new ByteBufOutputStream(compressedBuf));
         this.save.save2d(new EntryLocation2D(pos.field_77276_a, pos.field_77275_b), compressedBuf.nioBuffer());
      } finally {
         compressedBuf.release();
      }
   }

   @Override
   public void writeCube(CubePos pos, NBTTagCompound nbt) throws IOException {
      ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();

      try {
         CompressedStreamTools.func_74799_a(nbt, new ByteBufOutputStream(compressedBuf));
         this.save.save3d(new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()), compressedBuf.nioBuffer());
      } finally {
         compressedBuf.release();
      }
   }

   @Override
   public void writeBatch(ICubicStorage.NBTBatch batch) throws IOException {
      Map<EntryLocation2D, ByteBuf> compressedColumns = Collections.emptyMap();
      Map<EntryLocation3D, ByteBuf> compressedCubes = Collections.emptyMap();

      try {
         compressedColumns = this.compressNBTForBatchWrite(batch.columns, pos -> new EntryLocation2D(pos.field_77276_a, pos.field_77275_b));
         compressedCubes = this.compressNBTForBatchWrite(batch.cubes, pos -> new EntryLocation3D(pos.getX(), pos.getY(), pos.getZ()));
         if (!compressedColumns.isEmpty()) {
            this.save.save2d(compressedColumns.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().nioBuffer())));
         }

         if (!compressedCubes.isEmpty()) {
            this.save.save3d(compressedCubes.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().nioBuffer())));
         }
      } finally {
         compressedColumns.values().forEach(ReferenceCounted::release);
         compressedCubes.values().forEach(ReferenceCounted::release);
      }
   }

   private <KI, KO> Map<KO, ByteBuf> compressNBTForBatchWrite(Map<KI, NBTTagCompound> nbt, Function<KI, KO> keyMappingFunction) throws IOException {
      if (nbt.isEmpty()) {
         return Collections.emptyMap();
      } else {
         try {
            return nbt.entrySet().parallelStream().collect(Collectors.toMap(entry -> keyMappingFunction.apply(entry.getKey()), entry -> {
               ByteBuf compressedBuf = UnpooledByteBufAllocator.DEFAULT.ioBuffer();

               ByteBuf e;
               try {
                  CompressedStreamTools.func_74799_a(entry.getValue(), new ByteBufOutputStream(compressedBuf));
                  e = compressedBuf.retain();
               } catch (IOException var6) {
                  throw new UncheckedIOException(var6);
               } finally {
                  compressedBuf.release();
               }

               return e;
            }));
         } catch (UncheckedIOException var4) {
            throw var4.getCause();
         }
      }
   }

   @Override
   public void forEachColumn(Consumer<ChunkPos> callback) throws IOException {
      this.save.getSaveSection2D().forAllKeys(pos -> callback.accept(new ChunkPos(pos.getEntryX(), pos.getEntryZ())));
   }

   @Override
   public void forEachCube(Consumer<CubePos> callback) throws IOException {
      this.save.getSaveSection3D().forAllKeys(pos -> callback.accept(new CubePos(pos.getEntryX(), pos.getEntryY(), pos.getEntryZ())));
   }

   @Override
   public void flush() throws IOException {
      this.save.flush();
   }

   @Override
   public void close() throws IOException {
      this.save.close();
      this.save = null;
   }
}
