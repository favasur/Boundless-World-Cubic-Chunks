package cubicchunks.regionlib.util;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Utils {
   public Utils() {
   }

   public static void createDirectories(Path dir) throws IOException {
      if (!Files.isDirectory(dir)) {
         createDirectories(dir.getParent());
         Files.createDirectory(dir);
      }
   }

   public static void readFully(ByteChannel src, ByteBuffer data) throws IOException {
      while (data.hasRemaining()) {
         src.read(data);
      }
   }

   public static void writeFully(ByteChannel dst, ByteBuffer data) throws IOException {
      while (data.hasRemaining()) {
         dst.write(data);
      }
   }

   public static void writeFully(GatheringByteChannel dst, ByteBuffer[] data) throws IOException {
      long totalRemaining = Arrays.stream(data).mapToLong(Buffer::remaining).sum();
      long totalWritten = 0L;

      while (totalWritten < totalRemaining) {
         totalWritten += dst.write(data);
      }
   }
}
