package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandTeleport.class})
public class MixinCommandTeleport {
   @Nullable
   private WeakReference<ICubicWorld> commandWorld;

   public MixinCommandTeleport() {
   }

   @Inject(
      method = {"execute"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/server/CommandTeleport;getEntity(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Lnet/minecraft/entity/Entity;",
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

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = -4096
      )}
   )
   private int getMinY(int original) {
      if (this.commandWorld == null) {
         return original;
      } else {
         ICubicWorld world = this.commandWorld.get();
         return world == null ? original : world.getMinHeight() + original;
      }
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = 4096
      )},
      expect = 2
   )
   private int getMaxY(int original) {
      if (this.commandWorld == null) {
         return original;
      } else {
         ICubicWorld world = this.commandWorld.get();
         return world == null ? original : world.getMaxHeight() + original;
      }
   }
}
