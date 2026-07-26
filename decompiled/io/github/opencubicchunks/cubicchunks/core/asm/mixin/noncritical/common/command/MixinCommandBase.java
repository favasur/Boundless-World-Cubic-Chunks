package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.lang.ref.WeakReference;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandBase.class})
public class MixinCommandBase {
   @Nonnull
   private static WeakReference<ICubicWorld> commandWorld = new WeakReference<>(null);

   public MixinCommandBase() {
   }

   @Inject(
      method = {"parseBlockPos"},
      at = {@At("HEAD")}
   )
   private static void parseBlockPosPre(ICommandSender sender, String[] args, int startIndex, boolean centerBlock, CallbackInfoReturnable<?> cbi) {
      commandWorld = new WeakReference<>((ICubicWorld)sender.func_130014_f_());
   }

   @ModifyArg(
      method = {"parseBlockPos"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/CommandBase;parseDouble(DLjava/lang/String;IIZ)D",
         ordinal = 1
      ),
      index = 2
   )
   private static int getMinY(int original) {
      ICubicWorld world = commandWorld.get();
      return world == null ? original : world.getMinHeight();
   }

   @ModifyArg(
      method = {"parseBlockPos"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/CommandBase;parseDouble(DLjava/lang/String;IIZ)D",
         ordinal = 1
      ),
      index = 3
   )
   private static int getMaxY(int original) {
      ICubicWorld world = commandWorld.get();
      return world == null ? original : world.getMaxHeight();
   }
}
