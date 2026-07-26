package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({SPacketChunkData.class})
public interface ISPacketChunkData {
   @Accessor
   void setChunkX(int var1);

   @Accessor
   void setChunkZ(int var1);

   @Accessor
   void setAvailableSections(int var1);

   @Accessor
   void setBuffer(byte[] var1);

   @Accessor
   void setTileEntityTags(List<NBTTagCompound> var1);

   @Accessor
   void setFullChunk(boolean var1);
}
