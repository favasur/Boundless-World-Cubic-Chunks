package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketCubicWorldData implements IMessage {
   private boolean isCubicWorld;
   private int minHeight;
   private int maxHeight;
   private int minGenerationHeight;
   private int maxGenerationHeight;

   public PacketCubicWorldData() {
   }

   public PacketCubicWorldData(WorldServer world) {
      this.minHeight = 0;
      this.maxHeight = 256;
      if (((ICubicWorld)world).isCubicWorld()) {
         this.isCubicWorld = true;
         this.minHeight = ((ICubicWorld)world).getMinHeight();
         this.maxHeight = ((ICubicWorld)world).getMaxHeight();
         if (world.func_175624_G() instanceof ICubicWorldType) {
            ICubicWorldType type = (ICubicWorldType)world.func_175624_G();
            IntRange range = type.calculateGenerationHeightRange(world);
            this.minGenerationHeight = range.getMin();
            this.maxGenerationHeight = range.getMax();
         } else {
            this.minGenerationHeight = 0;
            this.maxGenerationHeight = 256;
         }
      }
   }

   public void fromBytes(ByteBuf buf) {
      this.isCubicWorld = buf.readBoolean();
      this.minHeight = buf.readInt();
      this.maxHeight = buf.readInt();
      this.minGenerationHeight = buf.readInt();
      this.maxGenerationHeight = buf.readInt();
   }

   public void toBytes(ByteBuf buf) {
      buf.writeBoolean(this.isCubicWorld);
      buf.writeInt(this.minHeight);
      buf.writeInt(this.maxHeight);
      buf.writeInt(this.minGenerationHeight);
      buf.writeInt(this.maxGenerationHeight);
   }

   public boolean isCubicWorld() {
      return this.isCubicWorld;
   }

   public int getMinHeight() {
      return this.minHeight;
   }

   public int getMaxHeight() {
      return this.maxHeight;
   }

   public int getMinGenerationHeight() {
      return this.minGenerationHeight;
   }

   public int getMaxGenerationHeight() {
      return this.maxGenerationHeight;
   }

   public static class Handler extends AbstractClientMessageHandler<PacketCubicWorldData> {
      public Handler() {
      }

      @Nullable
      public void handleClientMessage(World world, EntityPlayer player, PacketCubicWorldData message, MessageContext ctx) {
         if (message.isCubicWorld() && !((ICubicWorld)world).isCubicWorld()) {
            ((ICubicWorldInternal.Client)world)
               .initCubicWorldClient(
                  new IntRange(message.getMinHeight(), message.getMaxHeight()),
                  new IntRange(message.getMinGenerationHeight(), message.getMaxGenerationHeight())
               );
            if (FMLClientHandler.instance().hasOptifine()) {
               Minecraft.func_71410_x().field_71438_f.func_72732_a((WorldClient)world);
            }
         }
      }
   }
}
