package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandTP;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandTP.class})
public class MixinCommandTP {
   @Nullable
   private WeakReference<ICubicWorld> commandWorld;

   public MixinCommandTP() {
   }

   @Inject(
      method = {"execute"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/CommandTP;getEntity(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Lnet/minecraft/entity/Entity;",
         ordinal = 0
      )}
   )
   private void postGetEntityInject(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo ci) {
      try {
         this.commandWorld = new WeakReference<>((ICubicWorld)CommandBase.func_184885_b(server, sender, args[0]).func_130014_f_());
      } catch (CommandException var6) {
         this.commandWorld = null;
      }
   }

   @Inject(
      method = {"execute"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/CommandTP;getCommandSenderAsPlayer(Lnet/minecraft/command/ICommandSender;)Lnet/minecraft/entity/player/EntityPlayerMP;",
         ordinal = 0
      )}
   )
   private void postGetEntityPlayerInject(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo ci) {
      try {
         this.commandWorld = new WeakReference<>((ICubicWorld)CommandBase.func_71521_c(sender).func_130014_f_());
      } catch (PlayerNotFoundException var6) {
         this.commandWorld = null;
      }
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = -4096
      )}
   )
   private int getMinY(int orig) {
      if (this.commandWorld == null) {
         return orig;
      } else {
         ICubicWorld world = this.commandWorld.get();
         return world == null ? orig : world.getMinHeight() + orig;
      }
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = 4096
      )}
   )
   private int getMaxY(int orig) {
      if (this.commandWorld == null) {
         return orig;
      } else {
         ICubicWorld world = this.commandWorld.get();
         return world == null ? orig : world.getMaxHeight() + orig - 256;
      }
   }
}
