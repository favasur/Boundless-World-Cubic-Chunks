package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class WorldEncoder {
   WorldEncoder() {
   }

   static void encodeCubes(PacketBuffer out, Collection<Cube> cubes) {
      cubes.forEach(cube -> {
         byte flags = 0;
         if (cube.isEmpty()) {
            flags = (byte)(flags | 1);
         }

         if (cube.getStorage() != null) {
            flags = (byte)(flags | 2);
         }

         if (cube.getBiomeArray() != null) {
            flags = (byte)(flags | 4);
         }

         out.writeByte(flags);
      });
      cubes.forEach(cube -> {
         if (!cube.isEmpty()) {
            cube.getStorage().func_186049_g().func_186009_b(out);
         }
      });
      cubes.forEach(cube -> {
         if (cube.getStorage() != null) {
            out.writeBytes(cube.getStorage().func_76661_k().func_177481_a());
         }
      });
      cubes.forEach(cube -> {
         if (cube.getStorage() != null && cube.getWorld().field_73011_w.func_191066_m()) {
            out.writeBytes(cube.getStorage().func_76671_l().func_177481_a());
         }
      });
      cubes.forEach(cube -> {
         if (!cube.isEmpty()) {
            byte[] heightmaps = ((ServerHeightMap)((IColumn)cube.getColumn()).getOpacityIndex()).getDataForClient();

            assert heightmaps.length == 1024;

            out.writeBytes(heightmaps);
         }
      });
      cubes.forEach(cube -> {
         if (cube.getBiomeArray() != null) {
            out.writeBytes(cube.getBiomeArray());
         }
      });
   }

   static void encodeColumn(PacketBuffer out, Chunk column) {
      out.writeBytes(column.func_76605_m());
   }

   static void decodeColumn(PacketBuffer in, Chunk column) {
      in.readBytes(column.func_76605_m());
   }

   static void decodeCube(PacketBuffer in, List<Cube> cubes) {
      cubes.stream().filter(Objects::nonNull).forEach(Cube::setClientCube);
      boolean[] isEmpty = new boolean[cubes.size()];
      boolean[] hasStorage = new boolean[cubes.size()];
      boolean[] hasCustomBiomeMap = new boolean[cubes.size()];

      for (int i = 0; i < cubes.size(); i++) {
         byte flags = in.readByte();
         isEmpty[i] = (flags & 1) != 0 || cubes.get(i) == null;
         hasStorage[i] = (flags & 2) != 0 && cubes.get(i) != null;
         hasCustomBiomeMap[i] = (flags & 4) != 0 && cubes.get(i) != null;
      }

      for (int i = 0; i < cubes.size(); i++) {
         if (hasStorage[i]) {
            Cube cube = cubes.get(i);
            ExtendedBlockStorage storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()), cube.getWorld().field_73011_w.func_191066_m());
            cube.setStorage(storage);
         }
      }

      for (int ix = 0; ix < cubes.size(); ix++) {
         if (!isEmpty[ix]) {
            cubes.get(ix).getStorage().func_186049_g().func_186010_a(in);
         }
      }

      for (int ixx = 0; ixx < cubes.size(); ixx++) {
         if (hasStorage[ixx]) {
            byte[] data = cubes.get(ixx).getStorage().func_76661_k().func_177481_a();
            in.readBytes(data);
         }
      }

      for (int ixxx = 0; ixxx < cubes.size(); ixxx++) {
         if (hasStorage[ixxx] && cubes.get(ixxx).getWorld().field_73011_w.func_191066_m()) {
            byte[] data = cubes.get(ixxx).getStorage().func_76671_l().func_177481_a();
            in.readBytes(data);
         }
      }

      for (int ixxxx = 0; ixxxx < cubes.size(); ixxxx++) {
         if (!isEmpty[ixxxx]) {
            Cube cube = cubes.get(ixxxx);
            byte[] heightmaps = new byte[1024];
            in.readBytes(heightmaps);
            ClientHeightMap coi = (ClientHeightMap)((IColumn)cube.getColumn()).getOpacityIndex();
            coi.setData(heightmaps);
            cube.getStorage().func_76672_e();
         }
      }

      for (int ixxxxx = 0; ixxxxx < cubes.size(); ixxxxx++) {
         if (hasCustomBiomeMap[ixxxxx]) {
            Cube cube = cubes.get(ixxxxx);
            byte[] blockBiomeArray = new byte[64];
            in.readBytes(blockBiomeArray);
            cube.setBiomeArray(blockBiomeArray);
         }
      }
   }

   static int getEncodedSize(Chunk column) {
      return column.func_76605_m().length;
   }

   static int getEncodedSize(Collection<Cube> cubes) {
      int size = 0;
      size += cubes.size();

      for (Cube cube : cubes) {
         if (!cube.isEmpty()) {
            size += cube.getStorage().func_186049_g().func_186018_a();
         }

         if (cube.getStorage() != null) {
            size += cube.getStorage().func_76661_k().func_177481_a().length;
            if (cube.getWorld().field_73011_w.func_191066_m()) {
               size += cube.getStorage().func_76671_l().func_177481_a().length;
            }
         }
      }

      size += 1024 * cubes.size();

      for (Cube cube : cubes) {
         byte[] biomeArray = cube.getBiomeArray();
         if (biomeArray != null) {
            size += biomeArray.length;
         }
      }

      return size;
   }

   static ByteBuf createByteBufForWrite(byte[] data) {
      ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
      bytebuf.writerIndex(0);
      return bytebuf;
   }

   static ByteBuf createByteBufForRead(byte[] data) {
      ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
      bytebuf.readerIndex(0);
      return bytebuf;
   }
}
