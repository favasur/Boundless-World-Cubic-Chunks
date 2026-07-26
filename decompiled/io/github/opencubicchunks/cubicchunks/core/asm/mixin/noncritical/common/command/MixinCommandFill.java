package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandFill;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandFill.class})
public class MixinCommandFill {
   @Nullable
   private WeakReference<ICubicWorld> commandWorld;
   private Integer minY;
   private Integer maxY;

   public MixinCommandFill() {
   }

   @Inject(
      method = {"execute"},
      at = {@At("HEAD")},
      require = 1
   )
   private void getWorldFromExecute(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo cbi) {
      this.commandWorld = new WeakReference<>((ICubicWorld)sender.func_130014_f_());
   }

   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
         expandZeroConditions = {Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO},
         ordinal = 0
      ), @Constant(
         intValue = 256,
         ordinal = 0
      )},
      slice = {@Slice(
         from = @At(
            value = "CONSTANT",
            args = {"stringValue=commands.fill.tooManyBlocks"}
         )
      )},
      require = 2
   )
   private int execute_getMinHeight(int original) {
      if (this.commandWorld == null) {
         return original;
      } else {
         ICubicWorld world = this.commandWorld.get();
         if (world == null) {
            return original;
         } else {
            return original == 0 ? world.getMinHeight() : world.getMaxHeight();
         }
      }
   }

   @Inject(
      method = {"execute"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/command/ICommandSender;getEntityWorld()Lnet/minecraft/world/World;"
      )},
      locals = LocalCapture.CAPTURE_FAILSOFT
   )
   private void onGetEntityWorld(
      MinecraftServer server,
      ICommandSender sender,
      String[] args,
      CallbackInfo c,
      BlockPos blockpos,
      BlockPos blockpos1,
      Block block,
      IBlockState iblockstate,
      BlockPos minPos,
      BlockPos maxPos,
      int i
   ) {
      this.minY = minPos.func_177956_o();
      this.maxY = maxPos.func_177956_o();
   }

   @Redirect(
      method = {"execute"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"
      )
   )
   private boolean isBlockLoadedCheckForHeightRangeRedirect(World world, BlockPos pos) {
      if (!((ICubicWorld)world).isCubicWorld()) {
         return world.func_175667_e(pos);
      } else if (this.minY == null) {
         assert this.maxY == null;

         return ((ICubicWorld)world).isBlockColumnLoaded(pos);
      } else {
         for (int blockY = this.minY; blockY <= this.maxY; blockY += 16) {
            if (!world.func_175667_e(new BlockPos(pos.func_177958_n(), blockY, pos.func_177952_p()))) {
               return false;
            }
         }

         return true;
      }
   }
}
