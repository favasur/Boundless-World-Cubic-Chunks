package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayer;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayerDigging;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketPlayerTryUseItemOnBlock;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketTabComplete;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketUpdateSign;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient.ICPacketVehicleMove;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IPositionPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NetHandlerPlayServer.class})
public class MixinNetHandlerPlayServer {
   @Shadow
   public EntityPlayerMP field_147369_b;
   @Shadow
   private Vec3d field_184362_y;
   @Shadow
   private int field_184363_z;

   public MixinNetHandlerPlayServer() {
   }

   @Inject(
      method = {"processCustomPayload"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   public void preprocessPacket(CPacketCustomPayload packet, CallbackInfo ci) {
      if (CubicChunksConfig.allowVanillaClients && "MC|Brand".equals(packet.func_149559_c())) {
         PacketBuffer packetbuffer = packet.func_180760_b();
         if (packetbuffer.func_150789_c(32767).contains("Geyser")) {
            VanillaNetworkHandler.addBedrockPlayer(this.field_147369_b);
            if (CubicChunksConfig.vanillaClients.horizontalSlices && CubicChunksConfig.vanillaClients.horizontalSlicesBedrockOnly) {
               WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
               if (!((ICubicWorld)world).isCubicWorld()) {
                  return;
               }

               VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
               vanillaHandler.updatePlayerPosition(
                  (PlayerCubeMap)world.func_184164_w(),
                  this.field_147369_b,
                  new CubePos(
                     Coords.blockToCube(this.field_147369_b.field_70165_t),
                     Coords.blockToCube(this.field_147369_b.field_70163_u),
                     Coords.blockToCube(this.field_147369_b.field_70161_v)
                  )
               );
            }
         }

         packetbuffer.resetReaderIndex();
      }
   }

   @Inject(
      method = {"processPlayerDigging"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   public void preprocessPacket(CPacketPlayerDigging packetIn, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            ((ICPacketPlayerDigging)packetIn).setPosition(vanillaHandler.modifyPositionC2S(packetIn.func_179715_a(), this.field_147369_b));
         }
      }
   }

   @Inject(
      method = {"processPlayer"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   public void preprocessPacket(CPacketPlayer packet, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            ICPacketPlayer p = (ICPacketPlayer)packet;
            BlockPos offset = vanillaHandler.getC2SOffset(this.field_147369_b);
            p.setX(p.getX() - (double)offset.func_177958_n());
            p.setY(p.getY() - (double)offset.func_177956_o());
            p.setZ(p.getZ() - (double)offset.func_177952_p());
         }
      }
   }

   @Inject(
      method = {"processTryUseItemOnBlock"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   private void preprocessPacket(CPacketPlayerTryUseItemOnBlock packetIn, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            ((ICPacketPlayerTryUseItemOnBlock)packetIn).setPosition(vanillaHandler.modifyPositionC2S(packetIn.func_187023_a(), this.field_147369_b));
         }
      }
   }

   @Inject(
      method = {"processTabComplete"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   private void preprocessPacket(CPacketTabComplete packetIn, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            BlockPos targetBlock = packetIn.func_179709_b();
            if (targetBlock != null) {
               ((ICPacketTabComplete)packetIn).setTargetBlock(vanillaHandler.modifyPositionC2S(targetBlock, this.field_147369_b));
            }
         }
      }
   }

   @Inject(
      method = {"processUpdateSign"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   private void preprocessPacket(CPacketUpdateSign packetIn, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            ((ICPacketUpdateSign)packetIn).setPos(vanillaHandler.modifyPositionC2S(packetIn.func_179722_a(), this.field_147369_b));
         }
      }
   }

   @Inject(
      method = {"processVehicleMove"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )}
   )
   private void preprocessPacket(CPacketVehicleMove packetIn, CallbackInfo ci) {
      WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
      if (((ICubicWorld)world).isCubicWorld()) {
         VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
         boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
         if (!hasCC) {
            ICPacketVehicleMove p = (ICPacketVehicleMove)packetIn;
            BlockPos offset = vanillaHandler.getC2SOffset(this.field_147369_b);
            p.setX(packetIn.func_187004_a() - (double)offset.func_177958_n());
            p.setY(packetIn.func_187002_b() - (double)offset.func_177956_o());
            p.setZ(packetIn.func_187003_c() - (double)offset.func_177952_p());
         }
      }
   }

   @Inject(
      method = {"processConfirmTeleport"},
      at = {@At(
         value = "INVOKE",
         shift = At.Shift.AFTER,
         target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"
      )},
      cancellable = true
   )
   public void preprocessTeleportConfirm(CPacketConfirmTeleport packetIn, CallbackInfo ci) {
      if (CubicChunksConfig.allowVanillaClients) {
         WorldServer world = (WorldServer)this.field_147369_b.field_70170_p;
         if (((ICubicWorld)world).isCubicWorld()) {
            VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
            boolean hasCC = vanillaHandler.hasCubicChunks(this.field_147369_b);
            if (!hasCC && vanillaHandler.receiveOffsetUpdateConfirm(this.field_147369_b, packetIn.func_186987_a())) {
               ci.cancel();
            }
         }
      }
   }

   @ModifyVariable(
      method = {"sendPacket"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Packet<?> onSendPacket(Packet<?> packetIn) {
      if (!CubicChunksConfig.allowVanillaClients) {
         return packetIn;
      } else {
         World world = this.field_147369_b.field_70170_p;
         if (!((ICubicWorld)world).isCubicWorld()) {
            return packetIn;
         } else {
            VanillaNetworkHandler vanillaHandler = ((ICubicWorldInternal.Server)world).getVanillaNetworkHandler();
            if (packetIn instanceof IPositionPacket) {
               if (!vanillaHandler.hasCubicChunks(this.field_147369_b)) {
                  BlockPos targetOffset = vanillaHandler.getS2COffset(this.field_147369_b);
                  if (((IPositionPacket)packetIn).hasPosOffset()) {
                     packetIn = this.copyPacket(packetIn);
                  }

                  ((IPositionPacket)packetIn).setPosOffset(targetOffset);
                  return packetIn;
               }

               if (((IPositionPacket)packetIn).hasPosOffset()) {
                  return this.copyPacket(packetIn);
               }
            }

            return packetIn;
         }
      }
   }

   private Packet<?> copyPacket(Packet<?> packetIn) {
      return VanillaNetworkHandler.copyPacket(packetIn);
   }
}
