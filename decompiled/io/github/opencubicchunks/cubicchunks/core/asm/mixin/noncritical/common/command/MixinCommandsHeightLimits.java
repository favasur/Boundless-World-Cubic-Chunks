package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandClone;
import net.minecraft.command.CommandCompare;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandClone.class, CommandCompare.class})
public class MixinCommandsHeightLimits {
   public MixinCommandsHeightLimits() {
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO},
         ordinal = 0
      )},
      slice = {@Slice(
         from = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;minY:I"
         )
      )}
   )
   @Group(
      name = "command_getMinY",
      min = 2,
      max = 2
   )
   private int command_getMinY1(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
      return ((ICubicWorld)sender.func_130014_f_()).getMinHeight();
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO},
         ordinal = 1
      )},
      slice = {@Slice(
         from = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;minY:I"
         )
      )}
   )
   @Group(
      name = "command_getMinY"
   )
   private int command_getMinY2(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
      return ((ICubicWorld)sender.func_130014_f_()).getMinHeight();
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = 256,
         ordinal = 0
      )},
      slice = {@Slice(
         from = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;maxY:I"
         )
      )}
   )
   @Group(
      name = "command_getMaxY",
      min = 2,
      max = 2
   )
   private int command_getMaxY1(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
      return ((ICubicWorld)sender.func_130014_f_()).getMaxHeight();
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         intValue = 256,
         ordinal = 1
      )},
      slice = {@Slice(
         from = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;maxY:I"
         )
      )}
   )
   @Group(
      name = "command_getMaxY"
   )
   private int command_getMaxY2(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
      return ((ICubicWorld)sender.func_130014_f_()).getMaxHeight();
   }
}
