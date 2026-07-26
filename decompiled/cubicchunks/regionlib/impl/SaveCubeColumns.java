package cubicchunks.regionlib.impl;

import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.util.Utils;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class SaveCubeColumns implements Flushable, Closeable {
   private final SaveSection2D saveSection2D;
   private final SaveSection3D saveSection3D;

   public SaveCubeColumns(SaveSection2D saveSection2D, SaveSection3D saveSection3D) {
      this.saveSection2D = saveSection2D;
      this.saveSection3D = saveSection3D;
   }

   public SaveSection2D getSaveSection2D() {
      return this.saveSection2D;
   }

   public SaveSection3D getSaveSection3D() {
      return this.saveSection3D;
   }

   public void save3d(EntryLocation3D location, ByteBuffer data) throws IOException {
      this.saveSection3D.save(location, data);
   }

   public void save3d(Map<EntryLocation3D, ByteBuffer> entries) throws IOException {
      this.saveSection3D.save(entries);
   }

   public void save2d(EntryLocation2D location, ByteBuffer data) throws IOException {
      this.saveSection2D.save(location, data);
   }

   public void save2d(Map<EntryLocation2D, ByteBuffer> entries) throws IOException {
      this.saveSection2D.save(entries);
   }

   public Optional<ByteBuffer> load(EntryLocation3D location, boolean createRegion) throws IOException {
      return this.saveSection3D.load(location, createRegion);
   }

   public Optional<ByteBuffer> load(EntryLocation2D location, boolean createRegion) throws IOException {
      return this.saveSection2D.load(location, createRegion);
   }

   public static SaveCubeColumns create(Path directory) throws IOException {
      Utils.createDirectories(directory);
      Path part2d = directory.resolve("region2d");
      Utils.createDirectories(part2d);
      Path part3d = directory.resolve("region3d");
      Utils.createDirectories(part3d);
      SaveSection2D section2d = SaveSection2D.createAt(part2d);
      SaveSection3D section3d = SaveSection3D.createAt(part3d);
      return new SaveCubeColumns(section2d, section3d);
   }

   @Override
   public void flush() throws IOException {
      this.saveSection2D.flush();
      this.saveSection3D.flush();
   }

   @Override
   public void close() throws IOException {
      this.saveSection2D.close();
      this.saveSection3D.close();
   }
}
