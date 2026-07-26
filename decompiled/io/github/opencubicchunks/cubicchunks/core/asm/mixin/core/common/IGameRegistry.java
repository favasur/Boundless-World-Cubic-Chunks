package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
   value = {GameRegistry.class},
   remap = false
)
public interface IGameRegistry {
   @Nullable
   @Accessor
   static List<IWorldGenerator> getSortedGeneratorList() {
      throw new Error("IGameRegistry failed to apply");
   }

   @Invoker("computeSortedGeneratorList")
   static void computeGenerators() {
      throw new Error("IGameRegistry failed to apply");
   }
}
