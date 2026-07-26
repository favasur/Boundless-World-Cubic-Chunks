package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.client.CPacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketPlayer.class})
public interface ICPacketPlayer {
   @Accessor("x")
   void setX(double var1);

   @Accessor("x")
   double getX();

   @Accessor("y")
   void setY(double var1);

   @Accessor("y")
   double getY();

   @Accessor("z")
   void setZ(double var1);

   @Accessor("z")
   double getZ();
}
