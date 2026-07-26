package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.client.CPacketVehicleMove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketVehicleMove.class})
public interface ICPacketVehicleMove {
   @Accessor("x")
   void setX(double var1);

   @Accessor("y")
   void setY(double var1);

   @Accessor("z")
   void setZ(double var1);
}
