package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({PathNavigateGround.class})
public abstract class MixinPathNavigateGround extends PathNavigate {
   public MixinPathNavigateGround(EntityLiving entitylivingIn, World worldIn) {
      super(entitylivingIn, worldIn);
   }

   @Overwrite
   public Path func_179680_a(BlockPos posIn) {
      BlockPos posOriginal = posIn;
      if (this.field_75513_b.func_180495_p(posIn).func_185904_a() == Material.field_151579_a) {
         BlockPos pos = posIn.func_177977_b();

         while (
            pos.func_177956_o() > ((IMinMaxHeight)this.field_75513_b).getMinHeight()
               && this.field_75513_b.func_175667_e(pos)
               && this.field_75513_b.func_180495_p(pos).func_185904_a() == Material.field_151579_a
         ) {
            pos = pos.func_177977_b();
         }

         if (pos.func_177956_o() > ((IMinMaxHeight)this.field_75513_b).getMinHeight() && this.field_75513_b.func_175667_e(pos)) {
            return super.func_179680_a(pos.func_177984_a());
         }

         pos = pos.func_177984_a();

         while (
            pos.func_177956_o() < ((IMinMaxHeight)this.field_75513_b).getMaxHeight()
               && this.field_75513_b.func_175667_e(pos)
               && this.field_75513_b.func_180495_p(pos).func_185904_a() == Material.field_151579_a
         ) {
            pos = pos.func_177984_a();
         }

         posIn = pos;
      }

      if (!this.field_75513_b.func_180495_p(posIn).func_185904_a().func_76220_a()) {
         return super.func_179680_a(posIn);
      } else {
         BlockPos pos = posIn.func_177984_a();

         while (
            pos.func_177956_o() < ((IMinMaxHeight)this.field_75513_b).getMaxHeight()
               && this.field_75513_b.func_175667_e(pos)
               && this.field_75513_b.func_180495_p(pos).func_185904_a().func_76220_a()
         ) {
            pos = pos.func_177984_a();
         }

         return pos.func_177956_o() < ((IMinMaxHeight)this.field_75513_b).getMaxHeight() && this.field_75513_b.func_175667_e(pos)
            ? super.func_179680_a(pos)
            : super.func_179680_a(posOriginal);
      }
   }
}
