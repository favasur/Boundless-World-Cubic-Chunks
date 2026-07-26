package io.github.opencubicchunks.cubicchunks.core.network;

import gnu.trove.TShortCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import io.github.opencubicchunks.cubicchunks.core.util.AddressTools;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketCubeBlockChange implements IMessage {
   int[] heightValues;
   CubePos cubePos;
   short[] localAddresses;
   IBlockState[] blockStates;

   public PacketCubeBlockChange() {
   }

   public PacketCubeBlockChange(Cube cube, TShortCollection localAddresses) {
      this.cubePos = cube.getCoords();
      this.localAddresses = localAddresses.toArray();
      this.blockStates = new IBlockState[localAddresses.size()];
      int i = localAddresses.size() - 1;

      TIntSet xzAddresses;
      for (xzAddresses = new TIntHashSet(); i >= 0; i--) {
         int localAddress = this.localAddresses[i];
         int x = AddressTools.getLocalX(localAddress);
         int y = AddressTools.getLocalY(localAddress);
         int z = AddressTools.getLocalZ(localAddress);
         this.blockStates[i] = cube.getBlockState(x, y, z);
         xzAddresses.add(AddressTools.getLocalAddress(x, z));
      }

      this.heightValues = new int[xzAddresses.size()];
      i = 0;

      for (TIntIterator it = xzAddresses.iterator(); it.hasNext(); i++) {
         int v = it.next();
         int height = ((IColumn)cube.getColumn()).getOpacityIndex().getTopBlockY(Coords.blockToLocal(v), Coords.blockToCube(v));
         v |= height << 8;
         this.heightValues[i] = v;
      }
   }

   public void fromBytes(ByteBuf in) {
      this.cubePos = new CubePos(in.readInt(), in.readInt(), in.readInt());
      short numBlocks = in.readShort();
      this.localAddresses = new short[numBlocks];
      this.blockStates = new IBlockState[numBlocks];

      for (int i = 0; i < numBlocks; i++) {
         this.localAddresses[i] = in.readShort();
         this.blockStates[i] = (IBlockState)Block.field_176229_d.func_148745_a(ByteBufUtils.readVarInt(in, 4));
      }

      int numHmapChanges = in.readUnsignedByte();
      this.heightValues = new int[numHmapChanges];

      for (int i = 0; i < numHmapChanges; i++) {
         this.heightValues[i] = in.readInt();
      }
   }

   public void toBytes(ByteBuf out) {
      out.writeInt(this.cubePos.getX());
      out.writeInt(this.cubePos.getY());
      out.writeInt(this.cubePos.getZ());
      out.writeShort(this.localAddresses.length);

      for (int i = 0; i < this.localAddresses.length; i++) {
         out.writeShort(this.localAddresses[i]);
         ByteBufUtils.writeVarInt(out, Block.field_176229_d.func_148747_b(this.blockStates[i]), 4);
      }

      out.writeByte(this.heightValues.length);

      for (int v : this.heightValues) {
         out.writeInt(v);
      }
   }

   public static class Handler extends AbstractClientMessageHandler<PacketCubeBlockChange> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketCubeBlockChange packet, MessageContext ctx) {
         WorldClient worldClient = (WorldClient)world;
         CubeProviderClient cubeCache = (CubeProviderClient)worldClient.func_72863_F();
         Cube cube = cubeCache.getCube(packet.cubePos);
         if (cube instanceof BlankCube) {
            CubicChunks.LOGGER.error("Ignored block update to blank cube {}", packet.cubePos);
         } else {
            ClientHeightMap index = (ClientHeightMap)((IColumn)cube.getColumn()).getOpacityIndex();

            for (int hmapUpdate : packet.heightValues) {
               int x = hmapUpdate & 15;
               int z = hmapUpdate >> 4 & 15;
               int height = hmapUpdate >> 8;
               index.setHeight(x, z, height);
            }

            for (int i = 0; i < packet.localAddresses.length; i++) {
               BlockPos pos = cube.localAddressToBlockPos(packet.localAddresses[i]);
               worldClient.func_73031_a(
                  pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()
               );
               worldClient.func_180501_a(pos, packet.blockStates[i], 3);
            }

            cube.getTileEntityMap().values().forEach(TileEntity::func_145836_u);
         }
      }
   }
}
