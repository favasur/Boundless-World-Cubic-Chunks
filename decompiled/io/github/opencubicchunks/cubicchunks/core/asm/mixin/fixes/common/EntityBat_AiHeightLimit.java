package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin({EntityBat.class})
public class EntityBat_AiHeightLimit extends EntityAmbientCreature {
   public EntityBat_AiHeightLimit(World worldIn) {
      super(worldIn);
   }

   @ModifyConstant(
      method = {"updateAITasks"},
      constant = {@Constant(
         intValue = 1,
         ordinal = 0
      )}
   )
   private int updateAITasks_getMinSpawnPositionY(int originalY) {
      return ((ICubicWorld)this.field_70170_p).getMinHeight() + originalY;
   }
}
