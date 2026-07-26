package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.util.PacketUtils;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketCubes implements IMessage {
   private CubePos[] cubePos;
   private byte[] data;
   private List<List<NBTTagCompound>> tileEntityTags;

   public PacketCubes() {
   }

   public PacketCubes(List<Cube> cubes) {
      cubes.sort(
         Comparator.<Cube>comparingInt(c -> c.getCoords().getY()).thenComparingInt(c -> c.getCoords().getX()).thenComparingInt(c -> c.getCoords().getZ())
      );
      this.cubePos = new CubePos[cubes.size()];

      for (int i = 0; i < cubes.size(); i++) {
         this.cubePos[i] = cubes.get(i).getCoords();
      }

      this.data = new byte[WorldEncoder.getEncodedSize(cubes)];
      PacketBuffer out = new PacketBuffer(WorldEncoder.createByteBufForWrite(this.data));
      WorldEncoder.encodeCubes(out, cubes);
      this.tileEntityTags = new ArrayList<>();
      cubes.forEach(
         cube -> this.tileEntityTags
               .add(cube.getTileEntityMap().values().stream().<NBTTagCompound>map(TileEntity::func_189517_E_).collect(Collectors.toList()))
      );
   }

   public void fromBytes(ByteBuf buf) {
      int cubeCount = buf.readUnsignedShort();
      this.cubePos = new CubePos[cubeCount];

      for (int i = 0; i < this.cubePos.length; i++) {
         this.cubePos[i] = PacketUtils.readCubePos(buf);
      }

      this.data = new byte[buf.readInt()];
      buf.readBytes(this.data);
      this.tileEntityTags = new ArrayList<>();

      for (int i = 0; i < cubeCount; i++) {
         int numTiles = buf.readInt();
         List<NBTTagCompound> tags = new ArrayList<>();

         for (int j = 0; j < numTiles; j++) {
            tags.add(ByteBufUtils.readTag(buf));
         }

         this.tileEntityTags.add(tags);
      }
   }

   public void toBytes(ByteBuf buf) {
      buf.writeShort(this.cubePos.length);

      for (CubePos pos : this.cubePos) {
         PacketUtils.write(buf, pos);
      }

      buf.writeInt(this.data.length);
      buf.writeBytes(this.data);
      this.tileEntityTags.forEach(tags -> {
         buf.writeInt(tags.size());
         tags.forEach(tag -> ByteBufUtils.writeTag(buf, tag));
      });
   }

   CubePos[] getCubePos() {
      return this.cubePos;
   }

   byte[] getData() {
      return this.data;
   }

   List<List<NBTTagCompound>> getTileEntityTags() {
      return this.tileEntityTags;
   }

   public static class Handler extends AbstractClientMessageHandler<PacketCubes> {
      public Handler() {
      }

      public void handleClientMessage(World world, EntityPlayer player, PacketCubes message, MessageContext ctx) {
         WorldClient worldClient = (WorldClient)player.func_130014_f_();
         CubeProviderClient cubeCache = (CubeProviderClient)worldClient.func_72863_F();
         CubePos[] cubePos = message.getCubePos();
         List<Cube> cubes = new ArrayList<>();

         for (CubePos pos : cubePos) {
            Cube cube = cubeCache.loadCube(pos);
            if (cube == null) {
               CubicChunks.LOGGER.error("Out of order cube received! No column for cube at {} exists!", pos);
            }

            cubes.add(cube);
         }

         byte[] data = message.getData();
         ByteBuf buf = WorldEncoder.createByteBufForRead(data);
         WorldEncoder.decodeCube(new PacketBuffer(buf), cubes);
         cubes.stream().filter(Objects::nonNull).forEach(Cube::markForRenderUpdate);
         message.getTileEntityTags().forEach(tags -> tags.forEach(tag -> {
               int blockX = tag.func_74762_e("x");
               int blockY = tag.func_74762_e("y");
               int blockZ = tag.func_74762_e("z");
               BlockPos posx = new BlockPos(blockX, blockY, blockZ);
               TileEntity tileEntity = worldClient.func_175625_s(posx);
               if (tileEntity != null) {
                  tileEntity.handleUpdateTag(tag);
               }
            }));
      }
   }
}
