package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({WalkNodeProcessor.class})
public abstract class MixinWalkNodeProcessor extends NodeProcessor {
   public MixinWalkNodeProcessor() {
   }

   @Redirect(
      method = {"getSafePoint"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z"
      ),
      require = 2
   )
   private boolean collidesWithAnyBlockRedirect(World worldIn, AxisAlignedBB aabb) {
      if (((ICubicWorld)worldIn).isCubicWorld()) {
         List<AxisAlignedBB> aabbList = new ArrayList<>();
         double minX = aabb.field_72340_a;
         double minY = aabb.field_72338_b;
         double minZ = aabb.field_72339_c;
         double maxX = aabb.field_72336_d;
         double maxY = aabb.field_72337_e;
         double maxZ = aabb.field_72334_f;
         int x1 = MathHelper.func_76128_c(minX) - 1;
         int y1 = MathHelper.func_76128_c(minY) - 1;
         int z1 = MathHelper.func_76128_c(minZ) - 1;
         int x2 = MathHelper.func_76143_f(maxX) + 1;
         int y2 = MathHelper.func_76143_f(maxY) + 1;
         int z2 = MathHelper.func_76143_f(maxZ) + 1;

         for (MutableBlockPos pos : MutableBlockPos.func_191531_b(x1, y1, z1, x2, y2, z2)) {
            IBlockState bstate = this.field_176169_a.func_180495_p(pos);
            bstate.func_185908_a(worldIn, pos, aabb, aabbList, this.field_186326_b, false);
            if (!aabbList.isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return worldIn.func_184143_b(aabb);
      }
   }

   @ModifyVariable(
      method = {"getPathNodeType"},
      at = @At("HEAD")
   )
   public IBlockAccess getPathNodeTypeFromOwnBlockAccess(IBlockAccess world) {
      return this.field_176169_a;
   }
}
