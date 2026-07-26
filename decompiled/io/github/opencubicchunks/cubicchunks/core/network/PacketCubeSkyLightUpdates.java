package io.github.opencubicchunks.cubicchunks.core.network;

import gnu.trove.list.TShortList;
import io.github.opencubicchunks.cubicchunks.api.util.Bits;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCubeSkyLightUpdates implements IMessage {
   private CubePos cube;
   private boolean isFullRelight;
   private byte[] data;

   public PacketCubeSkyLightUpdates() {
   }

   public PacketCubeSkyLightUpdates(Cube cube, TShortList updates) {
      if (cube.getStorage() == null) {
         this.isFullRelight = true;
         this.data = null;
      } else {
         this.cube = cube.getCoords();
         this.data = new byte[updates.size() * 2];

         for (int i = 0; i < updates.size(); i++) {
            short packed = updates.get(i);
            int localX = AddressTools.getLocalX(packed);
            int localY = AddressTools.getLocalY(packed);
            int localZ = AddressTools.getLocalZ(packed);
            int value = cube.getStorage().func_76670_c(localX, localY, localZ);
            byte byte1 = (byte)(Bits.packUnsignedToInt(localX, 4, 0) | Bits.packUnsignedToInt(localY, 4, 4));
            byte byte2 = (byte)(Bits.packUnsignedToInt(localZ, 4, 0) | Bits.packUnsignedToInt(value, 4, 4));
            this.data[i * 2] = byte1;
            this.data[i * 2 + 1] = byte2;
         }
      }
   }

   public PacketCubeSkyLightUpdates(Cube cube) {
      this.isFullRelight = true;
      if (cube.getStorage() == null) {
         this.data = null;
      } else {
         this.cube = cube.getCoords();
         this.data = Arrays.copyOf(cube.getStorage().func_76671_l().func_177481_a(), 2048);
      }
   }

   public void fromBytes(ByteBuf buf) {
      this.cube = new CubePos(buf.readInt(), buf.readInt(), buf.readInt());
      this.isFullRelight = buf.readBoolean();
      boolean hasData = buf.readBoolean();
      if (hasData) {
         int size = ByteBufUtils.readVarInt(buf, 3);
         this.data = new byte[size];
         buf.readBytes(this.data);
      }
   }

   public void toBytes(ByteBuf buf) {
      buf.writeInt(this.cube.getX());
      buf.writeInt(this.cube.getY());
      buf.writeInt(this.cube.getZ());
      buf.writeBoolean(this.isFullRelight);
      buf.writeBoolean(this.data != null);
      if (this.data != null) {
         ByteBufUtils.writeVarInt(buf, this.data.length, 3);
         buf.writeBytes(this.data);
      }
   }

   CubePos getCubePos() {
      return this.cube;
   }

   boolean isFullRelight() {
      return this.isFullRelight;
   }

   byte[] getData() {
      return this.data;
   }

   public int updateCount() {
      return this.data.length / 2;
   }

   public static class Handler extends AbstractClientMessageHandler<PacketCubeSkyLightUpdates> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketCubeSkyLightUpdates message, MessageContext ctx) {
         WorldClient worldClient = (WorldClient)world;
         CubeProviderClient cubeCache = (CubeProviderClient)worldClient.func_72863_F();
         Cube cube = cubeCache.getCube(message.getCubePos());
         if (message.getData() == null) {
            cube.setStorage(Chunk.field_186036_a);
         } else {
            ExtendedBlockStorage storage = cube.getStorage();
            if (cube.getStorage() == null) {
               cube.setStorage(storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()), worldClient.field_73011_w.func_191066_m()));
            }

            assert storage != null;

            if (message.isFullRelight()) {
               storage.func_76666_d(new NibbleArray(message.getData()));
            } else {
               for (int i = 0; i < message.updateCount(); i++) {
                  int packed1 = message.getData()[i * 2] & 255;
                  int packed2 = message.getData()[i * 2 + 1] & 255;
                  storage.func_76657_c(
                     Bits.unpackUnsigned(packed1, 4, 0),
                     Bits.unpackUnsigned(packed1, 4, 4),
                     Bits.unpackUnsigned(packed2, 4, 0),
                     Bits.unpackUnsigned(packed2, 4, 4)
                  );
               }
            }

            LightingManager.CubeLightUpdateInfo info = cube.getCubeLightUpdateInfo();
            if (info != null) {
               info.clear();
            }

            cube.markForRenderUpdate();
         }
      }
   }
}
