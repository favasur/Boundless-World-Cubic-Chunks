package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({RandomPositionGenerator.class})
public class MixinRandomPositionGenerator {
   public MixinRandomPositionGenerator() {
   }

   @Overwrite
   private static BlockPos func_191378_a(BlockPos pos, EntityCreature entity) {
      if (!entity.field_70170_p.func_180495_p(pos).func_185904_a().func_76220_a()) {
         return pos;
      } else {
         BlockPos currentPos = pos.func_177984_a();

         while (
            currentPos.func_177956_o() < entity.field_70170_p.func_72800_K()
               && entity.field_70170_p.func_175667_e(currentPos)
               && entity.field_70170_p.func_180495_p(currentPos).func_185904_a().func_76220_a()
         ) {
            currentPos = currentPos.func_177984_a();
         }

         return !entity.field_70170_p.func_175667_e(currentPos) ? pos : currentPos;
      }
   }
}
